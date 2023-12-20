package ai.devchat.devchat.handler

import ai.devchat.cli.DevChatResponse
import ai.devchat.common.Log
import ai.devchat.devchat.BaseActionHandler
import ai.devchat.devchat.DevChatActions
import com.alibaba.fastjson.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.lang.Exception

class SendMessageRequestHandler(metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(metadata, payload) {
    override val actionName: String = DevChatActions.SEND_MESSAGE_RESPONSE

    private var currentChunkId = 0

    override fun action() {
        val flags: MutableList<Pair<String, String?>> = mutableListOf()

        val contexts = payload!!.getJSONArray("contexts")
        contexts?.takeIf { it.isNotEmpty() }?.forEachIndexed { i, _ ->
            val context = contexts.getJSONObject(i)
            val contextType = context.getString("type")

            val contextPath = when (contextType) {
                "code" -> {
                    val path = context.getString("path")
                    val filename = path.substring(path.lastIndexOf("/") + 1)
                    createTempFileFromContext(context, filename)
                }
                "command" -> createTempFileFromContext(context, "custom.txt")
                else -> null
            }

            contextPath?.let {
                flags.add("context" to it)
                Log.info("Context file path: $it")
            }
        }
        metadata!!.getString("parent")?.takeIf { it.isNotEmpty() }?.let {
            flags.add("parent" to it)
        }
        payload.getString("model")?.takeIf { it.isNotEmpty() }?.let {
            flags.add("model" to it)
        }

        val response = DevChatResponse()
        wrapper.route(flags, payload.getString("message")) {line ->
            response.update(line)
            promptCallback(response)
        }
        /* TODO: update messages cache with new one
        val currentTopic = ActiveConversation.topic ?: response.promptHash!!
        val newMessage = wrapper.logTopic(currentTopic, 1).getJSONObject(0)

        if (currentTopic == ActiveConversation.topic) {
            ActiveConversation.addMessage(newMessage)
        } else {
            ActiveConversation.reset(currentTopic, listOf(newMessage))
        }
         */
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

    private fun createTempFileFromContext(context: JSONObject, filename: String): String? {
        val tempFile: File = try {
            File.createTempFile("devchat-tmp-", "-$filename")
        } catch (e: IOException) {
            Log.error("Failed to create a temporary file." + e.message)
            return null
        }
        val newJson = JSONObject()
        if (context.getString("type") == "code") {
            newJson["languageId"] = context.getString("languageId")
            newJson["path"] = context.getString("path")
            newJson["startLine"] = context.getInteger("startLine")
            newJson["content"] = context.getString("content")
        } else if (context.getString("type") == "command") {
            newJson["command"] = context.getString("command")
            newJson["content"] = context.getString("content")
        }
        try {
            FileWriter(tempFile).use { fileWriter -> fileWriter.write(newJson.toJSONString()) }
        } catch (e: IOException) {
            Log.error("Failed to write to the temporary file." + e.message)
        }
        return tempFile.absolutePath
    }
}
