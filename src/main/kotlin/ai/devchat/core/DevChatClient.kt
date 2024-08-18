package ai.devchat.core

import ai.devchat.common.HttpClient
import ai.devchat.common.Log
import ai.devchat.common.PathUtils
import ai.devchat.plugin.localServicePort
import ai.devchat.storage.CONFIG
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import kotlin.system.measureTimeMillis


inline fun <reified T> T.asMap(): Map<String, Any?> where T : @Serializable Any {
    val json = Json { encodeDefaults = true }
    val jsonString = json.encodeToString(serializer(),this)
    return Json.decodeFromString<JsonObject>(jsonString).toMap()
}

@Serializable
data class ChatRequest(
    val content: String,
    @SerialName("model_name") val modelName: String,
    @SerialName("api_key") val apiKey: String,
    @SerialName("api_base") val apiBase: String,
    val parent: String?,
    val context: List<String>?,
    val workspace: String? = PathUtils.workspace,
    @Transient val contextContents: List<String>? = null,
    @Transient val response: ChatResponse? = null,
)
@Serializable
data class ChatResponse(
    var chunkId: Int = 0,
    @SerialName("prompt_hash") var promptHash: String? = null,
    var user: String? = null,
    var date: String? = null,
    var content: String? = "",
    @SerialName("finish_reason") var finishReason: String? = "",
    @SerialName("is_error") var isError: Boolean = false,
    var extra: JsonElement? = null
) {
    fun reset() {
        chunkId = 0
        promptHash = null
        user = null
        date = null
        content = ""
        finishReason = ""
        isError = false
        extra = null
    }

    fun appendChunk(chunk: Any) : ChatResponse {
        return when (chunk) {
            is String -> appendChunk(chunk)
            is ChatResponse -> appendChunk(chunk)
            else -> this
        }
    }

    private fun appendChunk(chunk: String) : ChatResponse {
        chunkId += 1;
        when {
            chunk.startsWith("User: ") -> user = user ?: chunk.substring("User: ".length)
            chunk.startsWith("Date: ") -> date = date ?: chunk.substring("Date: ".length)
            // 71 is the length of the prompt hash
            chunk.startsWith("prompt ") && chunk.length == 71 -> {
                promptHash = chunk.substring("prompt ".length)
                content = content?.let { "$it\n" } ?: "\n"
            }
            chunk.isNotEmpty() -> content = content?.let { "$it\n$chunk" } ?: chunk
        }
        return this
    }

    private fun appendChunk(chunk: ChatResponse) : ChatResponse {
        chunkId += 1;
        if (user == null) user = chunk.user
        if (date == null) date = chunk.date
        finishReason = chunk.finishReason
        if (finishReason == "should_run_workflow") {
            extra = chunk.extra
            Log.debug("should run workflow via cli.")
            return this
        }
        isError = chunk.isError
        content += chunk.content
        return this
    }
}
@Serializable
data class LogEntry(
    val model: String,
    val parent: String?,
    val messages: MutableList<Message>,
    val timestamp: Long,
    @SerialName("request_tokens") val requestTokens: Int,
    @SerialName("response_tokens") val responseTokens: Int
) {
    constructor(
        model: String,
        parent: String?,
        request: String,
        contexts: List<String>?,
        response: String?,
    ) : this(
        model, parent, mutableListOf(), Instant.now().epochSecond, 1, 1
    ) {
        this.messages.add(Message("user", request))
        this.messages.add(Message("assistant", response))
        this.messages.addAll(contexts?.map {
            Message("system", "<context>$it</context>")
        }.orEmpty())
    }
}

@Serializable
data class LogInsertRes(
    val hash: String? = null,
    val error: String? = null
)
@Serializable
data class LogDeleteRes(
    val success: Boolean? = null,
    val error: String? = null
)
@Serializable
data class Message(
    val role: String,
    val content: String?,
)

@Serializable
data class ShortLog(
    val hash: String,
    val parent: String?,
    val user: String,
    val date: Long,
    val request: String,
    val responses: List<String>,
    val context: List<Message>?,
    @SerialName("request_tokens") val requestTokens: Int,
    @SerialName("response_tokens") val responseTokens: Int
)

@Serializable
data class Topic(
    @SerialName("latest_time") val latestTime: Long,
    val hidden: Boolean,
    @SerialName("root_prompt_hash") val rootPromptHash: String,
    @SerialName("root_prompt_user") val rootPromptUser: String,
    @SerialName("root_prompt_date") val rootPromptDate: Long,
    @SerialName("root_prompt_request") val rootPromptRequest: String,
    @SerialName("root_prompt_response") val rootPromptResponse: String,
    val title: String?
)

@Serializable
data class CommandConf(
    val description: String,
    val help: String? = null,
)
@Serializable
data class Workflow(
    val name: String,
    val namespace: String,
    val active: Boolean,
    @SerialName("command_conf") val commandConf: CommandConf,
)

@Serializable
data class WorkflowConfig(
    val recommend: Recommend,
) {
    @Serializable
    data class Recommend(val workflows: List<String>)
}

@Serializable
data class UpdateWorkflowResponse(
    val updated: Boolean,
    val message: String? = null,
)

fun timeThis(block: suspend () -> Unit) {
    runBlocking {
        val time = measureTimeMillis {
            block()
        }
        Log.debug("Execution time: ${time / 1000.0} seconds")
    }
}


class DevChatClient {
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private val baseURL get() =  "http://localhost:$localServicePort"
    private var job: Job? = null

    val client: HttpClient = HttpClient()

    companion object {
        const val DEFAULT_LOG_MAX_COUNT = 10000
        const val LOG_RAW_DATA_SIZE_LIMIT = 4 * 1024 // 4kb
        const val RETRY_INTERVAL: Long = 500  // ms
        const val MAX_RETRIES: Int = 10
    }
    private val json = Json { ignoreUnknownKeys = true }


    fun message(
        message: ChatRequest,
        onData: (ChatResponse) -> Unit,
        onError: (String) -> Unit,
        onFinish: (Int) -> Unit,
    ) {
        cancelMessage()
        job = scope.launch {
            client.streamPost<ChatRequest, ChatResponse>("$baseURL/message/msg", message)
                .catch { e ->
                    onError(e.toString())
                    Log.warn("Error on sending message: $e")
                    onFinish(1)
                    cancelMessage()
                }
                .collect { chunk ->
                    if (chunk.finishReason == "should_run_workflow") {
                        onFinish(-1)
                        cancelMessage()
                    }
                    onData(chunk)
                }
            onFinish(0)
        }
    }

    fun getWorkflowList(): List<Workflow>? {
        return client.get("$baseURL/workflows/list")
    }
    fun getWorkflowConfig(): WorkflowConfig? {
        return client.get("$baseURL/workflows/config")
    }
    fun updateWorkflows() {
        val response: UpdateWorkflowResponse? = client.post<String?, _>("$baseURL/workflows/update")
        Log.info("Update workflows response: $response")
    }

    fun insertLog(logEntry: LogEntry): LogInsertRes? {
        val body = mutableMapOf("workspace" to PathUtils.workspace)
        val jsonData = json.encodeToString(serializer(), logEntry)
        if (jsonData.length <= LOG_RAW_DATA_SIZE_LIMIT) {
            body["jsondata"] = jsonData
        } else {
            body["filepath"] = PathUtils.createTempFile(jsonData, "devchat_log_insert_", ".json")
        }
        val response: LogInsertRes? = client.post("$baseURL/logs/insert", body)
        if (body.containsKey("filepath")) {
            try {
                Files.delete(Paths.get(body["filepath"]!!))
            } catch (e: Exception) {
                Log.error("Failed to delete temp file ${body["filepath"]}: $e")
            }
        }
        return response
    }
    fun deleteLog(logHash: String): LogDeleteRes? {
         return client.post("$baseURL/logs/delete", mapOf(
            "workspace" to PathUtils.workspace,
            "hash" to logHash
        ))
    }
    fun getTopicLogs(topicRootHash: String, offset: Int = 0, limit: Int = DEFAULT_LOG_MAX_COUNT): List<ShortLog> {
        return client.get<List<ShortLog>>("$baseURL/topics/$topicRootHash/logs", mapOf(
                "limit" to limit,
                "offset" to offset,
                "workspace" to PathUtils.workspace,
            )) ?: emptyList()
    }
    fun getTopics(offset: Int = 0, limit: Int = DEFAULT_LOG_MAX_COUNT): List<Topic> {
        val queryParams = mapOf(
            "limit" to limit,
            "offset" to offset,
            "workspace" to PathUtils.workspace,
        )
        return client.get<List<Topic>?>("$baseURL/topics", queryParams).orEmpty()
    }

    fun deleteTopic(topicRootHash: String) {
        val response: Map<String, String>? = client.post("$baseURL/topics/delete", mapOf(
            "topic_hash" to topicRootHash,
            "workspace" to PathUtils.workspace,
        ))
        Log.info("deleteTopic response data: $response")
    }

    fun getWebappUrl(): String? {
        val apiBase = CONFIG["providers.devchat.api_base"] as String
        val urlOrPath: String? = client.get("$apiBase/addresses/webapp")
        if (urlOrPath.isNullOrEmpty()) {
            Log.warn("No webapp url found");
            return null
        }
        var href = ""
        href = if (urlOrPath.startsWith("http://") || urlOrPath.startsWith("https://")) {
            urlOrPath
        } else {
            URL(URL(apiBase), urlOrPath).toString()
        }
        if (href.endsWith('/')) {
            href = href.dropLast(1)
        }
        if (href.endsWith("/api")) {
            href = href.dropLast(4)
        }
        Log.info("Webapp url: $href")
        return href
    }

    fun getIconUrl(): String {
        try {
            val webappUrl = getWebappUrl()
            if (!webappUrl.isNullOrEmpty()) {
                val iconsUrl = URL(URL(webappUrl), "/api/v1/plugin/icons/")
                val res: Map<String, String?>? = client.get(URL(iconsUrl, "filename/intellij").toString())
                res?.get("filename")?.let {
                    return URL(iconsUrl, it).toString()
                }
            }
        } catch (e: Exception) {
            Log.warn(e.toString())
        }
        return "/icons/pluginIcon_dark.svg"
    }

    private fun cancelMessage() {
        job?.cancel()
        job = null
    }
}

val DC_CLIENT: DevChatClient = DevChatClient()