package ai.devchat.core.handlers

import ai.devchat.common.Log
import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.plugin.currentProject
import com.alibaba.fastjson.JSONObject
import java.io.BufferedReader
import java.io.File

class CommitCodeRequestHandler(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.COMMIT_CODE_RESPONSE

    override fun action() {
        val message = payload!!.getString("message")
        val command = arrayOf("git", "commit", "-m", message)
        val projectDir = currentProject?.basePath
        val process = Runtime.getRuntime().exec(command, null, projectDir?.let { File(it) })
        val result = process.inputStream.bufferedReader().use(BufferedReader::readText)
        val errors = process.errorStream.bufferedReader().use(BufferedReader::readText)
        process.waitFor()
        val exitCode = process.exitValue()
        if (exitCode != 0) {
            throw RuntimeException("Failed to execute command: $command, Exit Code: $exitCode Error: $errors")
        }
        Log.info(result)
        super.action()
    }
}