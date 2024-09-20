package ai.devchat.core

import ai.devchat.common.Log
import ai.devchat.common.PathUtils
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
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
    val workspace: String? = null,
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

class DevChatClient(val project: Project, private val localServicePort: Int) {
    private val baseURL get() =  "http://localhost:$localServicePort"
    private var job: Job? = null
    private val workspace: String? = project.basePath

    companion object {
        const val DEFAULT_LOG_MAX_COUNT = 10000
        const val LOG_RAW_DATA_SIZE_LIMIT = 4 * 1024 // 4kb
        const val RETRY_INTERVAL: Long = 500  // ms
        const val MAX_RETRIES: Int = 50
    }
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    private inline fun <reified T> get(
        path: String,
        queryParams: Map<String, Any?> = emptyMap()
    ): T? {
        Log.info("GET request to [$baseURL$path] with request parameters: $queryParams")
        val urlBuilder = "$baseURL$path".toHttpUrlOrNull()?.newBuilder() ?: return null
        queryParams.forEach { (k, v) -> urlBuilder.addQueryParameter(k, v.toString()) }
        val url = urlBuilder.build()
        val request = Request.Builder().url(url).get().build()
        var retries = MAX_RETRIES
        while (retries > 0) {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException(
                        "Unsuccessful response: ${response.code} ${response.message}"
                    )
                    val result = response.body?.string()?.let {
                        json.decodeFromString<T>(it)
                    }
                    return result
                }
            } catch (e: IOException) {
                Log.warn("$e, retrying...")
                retries--
                Thread.sleep(RETRY_INTERVAL)
            } catch (e: Exception) {
                Log.warn(e.toString())
                return null
            }
        }
        return null
    }

    private inline fun <reified T, reified R> post(path: String, body: T? = null): R? {
        Log.info("POST request to [$baseURL$path] with request body: $body")
        val url = "$baseURL$path".toHttpUrlOrNull() ?: return null
        val requestBody = json.encodeToString(serializer(), body).toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()
        var retries = MAX_RETRIES
        while (retries > 0) {
            try {
                val response: Response = client.newCall(request).execute()
                if (!response.isSuccessful) throw IOException(
                    "Unsuccessful response: ${response.code} ${response.message}"
                )
                val result = response.body?.let {
                    json.decodeFromString<R>(it.string())
                }
                return result
            } catch (e: IOException) {
                Log.warn("$e, retrying...")
                retries--
                Thread.sleep(RETRY_INTERVAL)
            } catch (e: Exception) {
                Log.warn(e.toString())
                return null
            }
        }
        return null
    }

    private inline fun <reified T, reified R> streamPost(path: String, body: T? = null): Flow<R> = callbackFlow<R> {
        Log.info("POST request to [$baseURL$path] with request body: $body")
        val url = "$baseURL$path".toHttpUrlOrNull() ?: return@callbackFlow
        val requestJson = json.encodeToString(serializer(), body)
        val requestBody = requestJson.toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()
        val call = client.newCall(request)

        val response = call.execute()
        if (!response.isSuccessful) {
            throw IOException("Unexpected code $response")
        }
        response.body?.byteStream()?.use {inputStream ->
            val buffer = ByteArray(8192) // 8KB buffer
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                val chunk = buffer.copyOf(bytesRead).toString(Charsets.UTF_8)
                send(json.decodeFromString(chunk))
            }
        }
        close()
        awaitClose { call.cancel() }
    }.flowOn(Dispatchers.IO)

    fun message(
        message: ChatRequest,
        onData: (ChatResponse) -> Unit,
        onError: (String) -> Unit,
        onFinish: (Int) -> Unit,
    ) {
        cancelMessage()
        job = CoroutineScope(Dispatchers.IO).launch {
            streamPost<ChatRequest, ChatResponse>("/message/msg", message)
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
        return get("/workflows/list")
    }
    fun getWorkflowConfig(): WorkflowConfig? {
        return get("/workflows/config")
    }
    fun updateWorkflows() {
        val response: UpdateWorkflowResponse? = post<String?, _>("/workflows/update")
        Log.info("Update workflows response: $response")
    }
    fun updateCustomWorkflows() {
        val response: UpdateWorkflowResponse? = post<String?, _>("/workflows/custom_update")
        Log.info("Update custom workflows response: $response")
    }

    fun insertLog(logEntry: LogEntry): LogInsertRes? {
        val body = mutableMapOf("workspace" to workspace)
        val jsonData = json.encodeToString(serializer(), logEntry)
        if (jsonData.length <= LOG_RAW_DATA_SIZE_LIMIT) {
            body["jsondata"] = jsonData
        } else {
            body["filepath"] = PathUtils.createTempFile(jsonData, "devchat_log_insert_", ".json")
        }
        val response: LogInsertRes? = post("/logs/insert", body)
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
         return post("/logs/delete", mapOf(
            "workspace" to workspace,
            "hash" to logHash
        ))
    }
    fun getTopicLogs(topicRootHash: String, offset: Int = 0, limit: Int = DEFAULT_LOG_MAX_COUNT): List<ShortLog> {
        return get<List<ShortLog>>("/topics/$topicRootHash/logs", mapOf(
                "limit" to limit,
                "offset" to offset,
                "workspace" to workspace,
            )) ?: emptyList()
    }
    fun getTopics(offset: Int = 0, limit: Int = DEFAULT_LOG_MAX_COUNT): List<Topic> {
        val queryParams = mapOf(
            "limit" to limit,
            "offset" to offset,
            "workspace" to workspace,
        )
        return get<List<Topic>?>("/topics", queryParams).orEmpty()
    }

    fun deleteTopic(topicRootHash: String) {
        val response: Map<String, String>? = post("/topics/delete", mapOf(
            "topic_hash" to topicRootHash,
            "workspace" to workspace,
        ))
        Log.info("deleteTopic response data: $response")
    }

    private fun cancelMessage() {
        job?.cancel()
        job = null
    }
}