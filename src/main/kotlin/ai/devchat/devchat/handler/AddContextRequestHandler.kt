package ai.devchat.devchat.handler

import ai.devchat.common.Log
import ai.devchat.devchat.ActionHandler
import ai.devchat.devchat.DevChatActionHandler
import ai.devchat.devchat.DevChatActions
import com.alibaba.fastjson.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

class AddContextRequestHandler(private val devChatActionHandler: DevChatActionHandler) : ActionHandler {
    private var metadata: JSONObject? = null
    private var payload: JSONObject? = null
    override fun executeAction() {
        Log.info("Handling add context request")
        val command = payload!!.getString("command")
        val result = StringBuilder()
        val error = StringBuilder()
        var reader: BufferedReader? = null
        var errorReader: BufferedReader? = null
        try {
            val projectDir = devChatActionHandler.project?.basePath
            val process = Runtime.getRuntime().exec(command, null, projectDir?.let { File(it) })
            reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                result.append(line).append("\n")
            }
            errorReader = BufferedReader(InputStreamReader(process.errorStream))
            while (errorReader.readLine().also { line = it } != null) {
                error.append(line).append("\n")
            }
        } catch (e: IOException) {
            error.append(e.message)
        } finally {
            if (reader != null) {
                try {
                    reader.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            if (errorReader != null) {
                try {
                    errorReader.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        val callbackFunc = metadata!!.getString("callback")
        if (error.isEmpty()) {
            val finalResult = result.toString()
            devChatActionHandler.sendResponse(
                DevChatActions.ADD_CONTEXT_RESPONSE,
                callbackFunc
            ) { metadata: JSONObject, payload: JSONObject ->
                metadata["status"] = "success"
                metadata["error"] = ""
                payload["command"] = command
                payload["content"] = finalResult
            }
        } else {
            val finalError = error.toString()
            devChatActionHandler.sendResponse(
                DevChatActions.ADD_CONTEXT_RESPONSE,
                callbackFunc
            ) { metadata: JSONObject, payload: JSONObject ->
                metadata["status"] = "error"
                metadata["error"] = finalError
                payload["command"] = command
                payload["content"] = ""
            }
        }
    }

    override fun setMetadata(metadata: JSONObject) {
        this.metadata = metadata
    }

    override fun setPayload(payload: JSONObject) {
        this.payload = payload
    }
}
