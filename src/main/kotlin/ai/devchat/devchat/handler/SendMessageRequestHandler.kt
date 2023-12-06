package ai.devchat.devchat.handler

import ai.devchat.cli.DevChatResponse
import ai.devchat.common.Log
import ai.devchat.devchat.BaseActionHandler
import ai.devchat.devchat.DevChatActions
import ai.devchat.idea.storage.ActiveConversation
import com.alibaba.fastjson.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.lang.Exception
import java.nio.file.Files

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


        Log.info("Preparing to retrieve the command in the message...")
        var message = payload.getString("message")
        message = handleCommandAndInstruct(message, flags)

        val response = DevChatResponse()
        wrapper.prompt(flags, message) {line ->
            response.update(line)
            promptCallback(response)
        }
        val currentTopic = ActiveConversation.topic ?: response.promptHash!!
        val newMessage = wrapper.logTopic(currentTopic, 1).getJSONObject(0)

        if (currentTopic == ActiveConversation.topic) {
            ActiveConversation.addMessage(newMessage)
        } else {
            ActiveConversation.reset(currentTopic, listOf(newMessage))
        }
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

    @Throws(IOException::class)
    private fun handleCommandAndInstruct(message: String, flags: MutableList<Pair<String, String?>>): String {
        var message = message
        val commandList = wrapper.commandList
        val commandNames = List(commandList.size) {i -> commandList.getJSONObject(i).getString("name")}
        Log.info("Command names list: " + commandNames.joinToString(", "))
        var runResult: String? = null

        // Loop through the command names and check if message starts with it
        for (command in commandNames) {
            if (message.startsWith("/$command ")) {
                if (message.length > command!!.length + 2) {
                    message = message.substring(command.length + 2) // +2 to take into account the '/' and the space ' '
                }
                runResult = wrapper.runCommand(listOf(command), null)
                break
            }
        }
        // If we didn't find a matching command, assume the default behavior
        if (runResult != null) {
            // Write the result to a temporary file
            val tempFile = Files.createTempFile("devchat_", ".tmp")
            Files.write(tempFile, runResult.toByteArray())

            // Add the temporary file path to the flags with key --instruct
            flags.add("instruct" to tempFile.toString())
        }
        return message
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
