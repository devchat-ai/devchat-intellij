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

class CommitCodeRequestHandler(private val devChatActionHandler: DevChatActionHandler) : ActionHandler {
    private var metadata: JSONObject? = null
    private var payload: JSONObject? = null
    override fun executeAction() {
        Log.info("Handling commit code request")
        val callbackFunc = metadata!!.getString("callback")
        val message = payload!!.getString("message")
        val commitCommand = arrayOf("git", "commit", "-m", message)
        val result = StringBuilder()
        val error = StringBuilder()
        var reader: BufferedReader? = null
        var errorReader: BufferedReader? = null
        try {
            val projectDir = devChatActionHandler.project?.basePath
            Log.info("Preparing to execute command: git commit -m $message in $projectDir")
            val process = Runtime.getRuntime().exec(commitCommand, null, projectDir?.let { File(it) })
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
        val processCommitResponse = { metadata: JSONObject, payload: JSONObject ->
            if (error.isEmpty()) {
                metadata["status"] = "success"
                metadata["error"] = ""
            } else {
                metadata["status"] = "error"
                metadata["error"] = error.toString()
            }
        }
        devChatActionHandler.sendResponse(DevChatActions.COMMIT_CODE_RESPONSE, callbackFunc, processCommitResponse)
    }

    override fun setMetadata(metadata: JSONObject) {
        this.metadata = metadata
    }

    override fun setPayload(payload: JSONObject) {
        this.payload = payload
    }
}
