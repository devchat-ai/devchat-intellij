package ai.devchat.core.handlers

import ai.devchat.common.Log
import ai.devchat.common.PathUtils
import ai.devchat.core.*
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

        val parent = metadata?.getString("parent_hash")?.takeUnless { it.isEmpty() }
        val model = payload?.getString("model")?.takeIf { it.isNotEmpty() } ?: defaultModel
        val message = payload?.getString("text")
        val (contextTempFilePaths, contextContents) = processContexts(
            payload?.getJSONArray("contextInfo")
        ).unzip()

        val chatRequest = ChatRequest(
            content=message!!,
            modelName = model,
            apiKey = CONFIG["providers.devchat.api_key"] as String,
            apiBase = CONFIG["providers.devchat.api_base"] as String,
            parent=parent,
            context = contextTempFilePaths,
            workspace = project.basePath,
            response = ChatResponse(),
            contextContents = contextContents,
        )

        client!!.message(
            chatRequest,
            dataHandler(chatRequest),
            ::errorHandler,
            finishHandler(chatRequest)
        )


    }

    override fun except(exception: Exception) {
        send(
            payload = mapOf(
                "command" to "receiveMessage",
                "text" to "Exception occurred while executing 'devchat' command. ${exception.message}",
                "isError" to true,
                "hash" to ""
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
        return { exitCode: Int ->
            when(exitCode) {
                0 -> {
                    val entry = client!!.insertLog(
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
                    val logs = client!!.getTopicLogs(currentTopic, 0, 1)

                    if (currentTopic == activeConversation!!.topic) {
                        activeConversation!!.addMessage(logs.first())
                    } else {
                        activeConversation!!.reset(currentTopic, logs)
                    }
                }
                -1 -> runWorkflow(chatRequest)
            }
        }
    }

    private fun errorHandler(e: String) {
        send(
            payload = mapOf(
                "command" to "receiveMessage",
                "text" to e,
                "isError" to true,
                "hash" to ""
            )
        )
    }

    private fun promptCallback(response: ChatResponse) {
        response.content?.let {
            currentChunkId += 1
            send(
                payload = mapOf(
                    "command" to if (response.promptHash != null) "receiveMessage" else "receiveMessagePartial",
                    "text" to response.content,
                    "hash" to response.promptHash,
                    "user" to response.user,
                    "date" to response.date
                )
            )
        }
    }

    private fun processContexts(contexts: List<Any>?): List<Pair<String, String>> {
        val prefix = "devchat-context-"
        return contexts?.mapNotNull {context ->
            val context = context as? JSONObject ?: return@mapNotNull null
            // context has two fields: file and context
            val contextMap = context.getJSONObject("context").entries.associate { (key, value) ->
                key to value.toString()
            }
            val data = json.encodeToString(serializer(), contextMap)
            val tempFilePath = PathUtils.createTempFile(data, prefix)
            Log.info("Context file path: $tempFilePath")
            tempFilePath!! to data
        }.orEmpty()
    }

    companion object {
        var lastRequestArgs: Pair<JSONObject?, JSONObject?>? = null
    }

}