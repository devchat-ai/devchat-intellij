package ai.devchat.core.handlers

import ai.devchat.common.Log
import ai.devchat.common.PathUtils
import ai.devchat.core.*
import ai.devchat.plugin.DevChatService
import ai.devchat.storage.CONFIG
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.project.Project
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

class SendMessageRequestHandler(project: Project, requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    project,
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.SEND_MESSAGE_RESPONSE
    private val defaultModel = CONFIG["default_model"] as String

    private var currentChunkId = 0
    private val json = Json { ignoreUnknownKeys=true }

    override fun action() {
        if (requestAction == DevChatActions.REGENERATION_REQUEST) {
            lastRequestArgs?.let {
                metadata = it.first
                payload = it.second
            }
        } else {
            lastRequestArgs = Pair(metadata, payload)
        }

        val parent = metadata!!.getString("parent")?.takeUnless { it.isEmpty() }
        val model = payload!!.getString("model")?.takeIf { it.isNotEmpty() } ?: defaultModel
        val message = payload!!.getString("message")
        val (contextTempFilePaths, contextContents) = processContexts(
            json.decodeFromString(
                payload!!.getJSONArray("contexts").toString()
            )
        ).unzip()

        val chatRequest = ChatRequest(
            content=message,
            modelName = model,
            apiKey = CONFIG["providers.devchat.api_key"] as String,
            apiBase = CONFIG["providers.devchat.api_base"] as String,
            parent=parent,
            context = contextTempFilePaths,
            workspace = project.basePath,
            response = ChatResponse(),
            contextContents = contextContents,
        )

        project.getService(DevChatService::class.java).client!!.message(
            chatRequest,
            dataHandler(chatRequest),
            ::errorHandler,
            finishHandler(chatRequest)
        )


    }

    override fun except(exception: Exception) {
        send(
            metadata=mapOf(
                "currentChunkId" to 0,
                "isFinalChunk" to true,
                "finishReason" to "error",
                "error" to "Exception occurred while executing 'devchat' command."
            )
        )
    }

    private fun runWorkflow(chatRequest: ChatRequest) {
        chatRequest.response!!.reset()
        val flags: List<Pair<String, String?>> = buildList {
            add("model" to chatRequest.modelName)
            chatRequest.parent?.let { add("parent" to it) }
            addAll(chatRequest.context?.map {"context" to it}.orEmpty())
        }

        wrapper!!.route(
            flags,
            chatRequest.content,
            callback = dataHandler(chatRequest),
            onError = ::errorHandler,
            onFinish = finishHandler(chatRequest)
        )
    }


    private fun dataHandler(chatRequest: ChatRequest): (Any) -> Unit {
        return { data: Any ->
            chatRequest.response!!.appendChunk(data)
            promptCallback(chatRequest.response)
        }
    }
    private fun finishHandler(chatRequest: ChatRequest): (Int) -> Unit {
        val response = chatRequest.response!!
        val devChatService = project.getService(DevChatService::class.java)
        return { exitCode: Int ->
            when(exitCode) {
                0 -> {
                    val entry = devChatService.client!!.insertLog(
                        LogEntry(
                            chatRequest.modelName,
                            chatRequest.parent,
                            chatRequest.content,
                            chatRequest.contextContents,
                            response.content
                        )
                    )
                    response.promptHash = entry!!.hash
                    promptCallback(response)

                    val currentTopic = activeConversation!!.topic ?: response.promptHash!!
                    val logs = devChatService.client!!.getTopicLogs(currentTopic, 0, 1)

                    if (currentTopic == activeConversation.topic) {
                        activeConversation.addMessage(logs.first())
                    } else {
                        activeConversation.reset(currentTopic, logs)
                    }
                }
                -1 -> runWorkflow(chatRequest)
            }
        }
    }

    private fun errorHandler(e: String) {
        send(metadata=mapOf(
            "currentChunkId" to 0,
            "isFinalChunk" to true,
            "finishReason" to "error",
            "error" to e
        ))
    }

    private fun promptCallback(response: ChatResponse) {
        response.content?.let {
            currentChunkId += 1
            send(
                payload = mapOf(
                    "message" to response.content,
                    "user" to response.user,
                    "date" to response.date,
                    "promptHash" to response.promptHash
                ),
                metadata = mapOf(
                    "currentChunkId" to currentChunkId,
                    "isFinalChunk" to (response.promptHash != null),
                    "finishReason" to if (response.promptHash != null) "success" else "",
                    "error" to ""
                ),
            )
        }
    }

    private fun processContexts(contexts: List<Map<String, String?>>?): List<Pair<String, String>> {
        val prefix = "devchat-context-"
        return contexts?.mapNotNull {context ->
            when (context["type"] as? String) {
                "code", "command" -> {
                    val data = json.encodeToString(serializer(), context)
                    val tempFilePath = PathUtils.createTempFile(data, prefix)
                    Log.info("Context file path: $tempFilePath")
                    tempFilePath!! to data
                }
                else -> null
            }
        }.orEmpty()
    }

    companion object {
        var lastRequestArgs: Pair<JSONObject?, JSONObject?>? = null
    }

}
