package ai.devchat.devchat.handler

import ai.devchat.cli.DevChatResponse
import ai.devchat.common.Log
import ai.devchat.devchat.BaseActionHandler
import ai.devchat.devchat.DevChatActions
import ai.devchat.idea.settings.DevChatSettingsState
import ai.devchat.idea.storage.ActiveConversation
import com.alibaba.fastjson.JSONObject
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.time.Instant

class SendMessageRequestHandler(metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(metadata, payload) {
    override val actionName: String = DevChatActions.SEND_MESSAGE_RESPONSE

    private var currentChunkId = 0

    override fun action() {
        val flags: MutableList<Pair<String, String?>> = mutableListOf()

        val contexts = payload!!.getJSONArray("contexts")
        val contextJSONs = mutableListOf<String>()
        contexts?.takeIf { it.isNotEmpty() }?.forEachIndexed { i, _ ->
            val context = contexts.getJSONObject(i)
            val contextType = context.getString("type")

            val contextPath = when (contextType) {
                "code" -> {
                    val filename = context.getString("path").substringAfterLast(".", "")
                    val str = listOf(
                        "languageId", "path", "startLine", "content"
                    ).fold(JSONObject()) { acc, key -> acc[key] = context[key]; acc }.toJSONString()
                    contextJSONs.add(str)
                    createTempFile(str, filename)
                }
                "command" -> {
                    val str = listOf(
                        "command", "content"
                    ).fold(JSONObject()) { acc, key -> acc[key] = context[key]; acc }.toJSONString()
                    contextJSONs.add(str)
                    createTempFile(str, "custom.txt")
                }
                else -> null
            }

            contextPath?.let {
                flags.add("context" to it)
                Log.info("Context file path: $it")
            }
        }
        val parent = metadata!!.getString("parent")
        parent?.takeIf { it.isNotEmpty() }?.let {
            flags.add("parent" to it)
        }
        val model = payload.getString("model")
        model?.takeIf { it.isNotEmpty() }?.let {
            flags.add("model" to it)
        }
        val message = payload.getString("message")

        val response = DevChatResponse()
        wrapper.route(
            flags,
            message,
            {line ->
                response.update(line)
                promptCallback(response)
            },
            { _ ->
                insertLog(
                    contextJSONs,
                    model.takeIf { it.isNotEmpty() } ?: DevChatSettingsState.instance.defaultModel,
                    message,
                    response.message ?: "",
                    parent
                )
                val lastRecord = wrapper.logLast()
                response.update("prompt ${lastRecord!!["hash"]}")
                promptCallback(response)

                val currentTopic = ActiveConversation.topic ?: response.promptHash!!
                val newMessage = wrapper.logTopic(currentTopic, 1).getJSONObject(0)

                if (currentTopic == ActiveConversation.topic) {
                    ActiveConversation.addMessage(newMessage)
                } else {
                    ActiveConversation.reset(currentTopic, listOf(newMessage))
                }
            }
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

    private fun promptCallback(response: DevChatResponse) {
        response.message?.let {
            currentChunkId += 1
            send(
                payload = mapOf(
                    "message" to response.message,
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

    private fun createTempFile(content: String, filename: String): String? {
        return try {
            val tempFile = File.createTempFile("devchat-tmp-", "-$filename")
            tempFile.writeText(content)
            tempFile.absolutePath
        } catch (e: IOException) {
            Log.error("Failed to create a temporary file." + e.message)
            return null
        }
    }

    private fun insertLog(
        contexts: List<String>?,
        model: String,
        request: String,
        response: String,
        parent: String?
    ) {
        val item = mutableMapOf<String, Any?>(
            "model" to model,
            "messages" to listOf(
                mutableMapOf(
                    "role" to "user",
                    "content" to request
                ),
                mutableMapOf(
                    "role" to "assistant",
                    "content" to response
                ),
                *contexts?.map { mapOf(
                    "role" to "system",
                    "content" to "<context>$it</context>"
                ) }.orEmpty().toTypedArray()
            ),
            "timestamp" to Instant.now().epochSecond,
            "request_tokens" to 1,
            "response_tokens" to 1,
        )
        parent?.let {item.put("parent", parent)}
        wrapper.logInsert(JSONObject(item).toJSONString())
    }

}
