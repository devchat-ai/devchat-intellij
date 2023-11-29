package ai.devchat.devchat.handler

import ai.devchat.cli.DevChatResponse
import ai.devchat.cli.DevChatResponseConsumer
import ai.devchat.cli.DevChatWrapper
import ai.devchat.common.DevChatPathUtil
import ai.devchat.common.Log
import ai.devchat.devchat.ActionHandler
import ai.devchat.devchat.DevChatActionHandler
import ai.devchat.devchat.DevChatActions
import ai.devchat.idea.setting.DevChatSettingsState
import ai.devchat.idea.storage.SensitiveDataStorage
import com.alibaba.fastjson.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.nio.file.Files
import java.util.function.Consumer

class SendMessageRequestHandler(private val devChatActionHandler: DevChatActionHandler) : ActionHandler {
    private var metadata: JSONObject? = null
    private var payload: JSONObject? = null
    private var currentChunkId = 0
    override fun executeAction() {
        Log.info("Handling send message request")
        var message = payload!!.getString("message")
        val parent = metadata!!.getString("parent")
        val callbackFunc = metadata!!.getString("callback")
        try {
            val flags: MutableMap<String, List<String?>> = HashMap()
            val contextArray = payload!!.getJSONArray("contexts")
            if (contextArray != null && contextArray.size > 0) {
                val contextFilePaths: MutableList<String?> = ArrayList()
                for (i in contextArray.indices) {
                    val context = contextArray.getJSONObject(i)
                    val contextType = context.getString("type")
                    var contextPath: String? = null
                    if ("code" == contextType) {
                        val path = context.getString("path")
                        val filename = path.substring(path.lastIndexOf("/") + 1, path.length)
                        contextPath = createTempFileFromContext(context, filename)
                    } else if ("command" == contextType) {
                        contextPath = createTempFileFromContext(context, "custom.txt")
                    }
                    if (contextPath != null) {
                        contextFilePaths.add(contextPath)
                        Log.info("Context file path: $contextPath")
                    }
                }
                flags["context"] = contextFilePaths
            }
            if (parent != null && !parent.isEmpty()) {
                flags["parent"] = listOf(parent)
            }
            Log.info("Preparing to retrieve the command in the message...")
            message = handleCommandAndInstruct(message, flags)
            Log.info("Message is: $message")
            val devchatCommandPath = DevChatPathUtil.devchatBinPath
            val apiKey = SensitiveDataStorage.getKey()
            var apiBase = ""
            if (apiKey.startsWith("sk-")) {
                apiBase = "https://api.openai.com/v1"
            } else if (apiKey.startsWith("DC.")) {
                apiBase = "https://api.devchat.ai/v1"
            }
            val settings = DevChatSettingsState.instance
            if (settings.apiBase != null && !settings.apiBase.isEmpty()) {
                apiBase = settings.apiBase
            } else {
                settings.apiBase = apiBase
            }
            val responseConsumer = getResponseConsumer(callbackFunc)
            val devchatWrapper = DevChatWrapper(apiBase, apiKey, settings.defaultModel, devchatCommandPath)
            devchatWrapper.runPromptCommand(flags, message, responseConsumer)
        } catch (e: Exception) {
            Log.error("Exception occurred while executing DevChat command. Exception message: " + e.message)
            devChatActionHandler.sendResponse(
                DevChatActions.SEND_MESSAGE_RESPONSE, callbackFunc
            ) { metadata: JSONObject, payload: JSONObject? ->
                metadata["currentChunkId"] = 0
                metadata["isFinalChunk"] = true
                metadata["finishReason"] = "error"
                metadata["error"] = "Exception occurred while executing 'devchat' command."
            }
        }
    }

    @Throws(IOException::class)
    private fun handleCommandAndInstruct(message: String, flags: MutableMap<String, List<String?>>): String {
        var message = message
        val devchatWrapper = DevChatWrapper(DevChatPathUtil.devchatBinPath)
        val commandNamesList = devchatWrapper.commandNamesList
        Log.info("Command names list: " + java.lang.String.join(", ", *commandNamesList))
        var runResult: String? = null

        // Loop through the command names and check if message starts with it
        for (command in commandNamesList) {
            if (message.startsWith("/$command ")) {
                if (message.length > command!!.length + 2) {
                    message = message.substring(command.length + 2) // +2 to take into account the '/' and the space ' '
                }
                runResult = devchatWrapper.runRunCommand(command, null)
                break
            }
        }
        // If we didn't find a matching command, assume the default behavior
        if (runResult != null) {
            // Write the result to a temporary file
            val tempFile = Files.createTempFile("devchat_", ".tmp")
            Files.write(tempFile, runResult.toByteArray())

            // Add the temporary file path to the flags with key --instruct
            flags["instruct"] = listOf(tempFile.toString())
        }
        return message
    }

    private fun getResponseConsumer(responseFunc: String): DevChatResponseConsumer {
        val jsCallback = Consumer { response: DevChatResponse ->
            devChatActionHandler.sendResponse(
                DevChatActions.SEND_MESSAGE_RESPONSE, responseFunc
            ) { metadata: JSONObject, payload: JSONObject ->
                currentChunkId += 1
                metadata["currentChunkId"] = currentChunkId
                metadata["isFinalChunk"] = response.promptHash != null
                metadata["finishReason"] = if (response.promptHash != null) "success" else ""
                metadata["error"] = ""
                payload["message"] = response.message
                payload["user"] = response.user
                payload["date"] = response.date
                payload["promptHash"] = response.promptHash
            }
        }
        return DevChatResponseConsumer(jsCallback)
    }

    private fun createTempFileFromContext(context: JSONObject, filename: String): String? {
        val tempFile: File
        tempFile = try {
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

    override fun setMetadata(metadata: JSONObject) {
        this.metadata = metadata
    }

    override fun setPayload(payload: JSONObject) {
        this.payload = payload
    }
}
