package ai.devchat.devchat.handler

import ai.devchat.common.ProjectUtils
import ai.devchat.devchat.BaseActionHandler
import ai.devchat.devchat.DevChatActions
import com.alibaba.fastjson.JSONObject
import java.io.BufferedReader
import java.io.File

class AddContextRequestHandler(metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(metadata, payload) {
    override val actionName: String = DevChatActions.ADD_CONTEXT_RESPONSE
    val command: String? = payload?.getString("command")

    override fun action() {
        val projectDir = ProjectUtils.project?.basePath
        val process = Runtime.getRuntime().exec(command, null, projectDir?.let { File(it) })
        val result = process.inputStream.bufferedReader().use(BufferedReader::readText)
        val errors = process.errorStream.bufferedReader().use(BufferedReader::readText)
        process.waitFor()
        val exitCode = process.exitValue()
        if (exitCode != 0) {
            throw RuntimeException("Failed to execute command: $command, Exit Code: $exitCode Error: $errors")
        }
        send(payload=mapOf("command" to command, "content" to result))
    }

    override fun except(exception: Exception) {
        send(
            metadata=mapOf("status" to "error", "error" to exception.message),
            payload=mapOf("command" to command, "content" to "")
        )
    }
}
