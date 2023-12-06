package ai.devchat.cli

import ai.devchat.common.DevChatPathUtil
import ai.devchat.common.Log
import ai.devchat.common.Settings
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.intellij.util.containers.addIfNotNull
import java.io.BufferedReader
import java.io.IOException

private const val DEFAULT_LOG_MAX_COUNT = 10000

class DevChatWrapper(
    private val command: String = DevChatPathUtil.devchatBinPath,
    private var apiBase: String? = null,
    private var apiKey: String? = null,
    private var currentModel: String? = null
) {
    init {
        if (apiBase.isNullOrEmpty() || apiKey.isNullOrEmpty() || currentModel.isNullOrEmpty()) {
            val (key, api, model) = Settings.getAPISettings()
            apiBase = apiBase ?: api
            apiKey = apiKey ?: key
            currentModel = currentModel ?: model
        }
    }

    private fun execCommand(commands: List<String>, callback: ((String) -> Unit)?): String? {
        val pb = ProcessBuilder(commands)
        val env = pb.environment()

        apiBase?.let {
            env["OPENAI_API_BASE"] = it
            Log.info("api_base: $it")
        }
        apiKey?.let {
            env["OPENAI_API_KEY"] = it
            Log.info("api_key: ${it.substring(0, 5)}...${it.substring(it.length - 4)}")
        }
        return try {
            Log.info("Executing command: ${commands.joinToString(" ")}}")
            val process = pb.start()
            val text = process.inputStream.bufferedReader().use { reader ->
                callback?.let {
                    reader.forEachLine(it)
                    ""
                } ?: reader.readText()
            }
            val errors = process.errorStream.bufferedReader().use(BufferedReader::readText)
            process.waitFor()
            val exitCode = process.exitValue()

            if (exitCode != 0) {
                Log.error("Failed to execute command: $commands Exit Code: $exitCode Error: $errors")
                throw RuntimeException(
                    "Failed to execute command: $commands Exit Code: $exitCode Error: $errors"
                )
            } else {
                text
            }
        } catch (e: IOException) {
            Log.error("Failed to execute command: $commands")
            throw RuntimeException("Failed to execute command: $commands", e)
        } catch (e: InterruptedException) {
            Log.error("Failed to execute command: $commands")
            throw RuntimeException("Failed to execute command: $commands", e)
        }
    }

    val prompt: (MutableList<Pair<String, String?>>, String, ((String) -> Unit)?) -> Unit get() = {
        flags: MutableList<Pair<String, String?>>, message: String, callback: ((String) -> Unit)? ->
            flags.addAll(listOf("model" to currentModel, "" to message))
            subCommand(listOf("prompt"))(flags, callback)
    }

    val logTopic: (String, Int?) -> JSONArray get() = {topic: String, maxCount: Int? ->
        val num: Int = maxCount ?: DEFAULT_LOG_MAX_COUNT
        JSON.parseArray(log(mutableListOf(
            "topic" to topic,
            "max-count" to num.toString()
        ), null))
    }

    val run get() = subCommand(listOf("run"))
    val log get() = subCommand(listOf("log"))
    val topic get() = subCommand(listOf("topic"))

    val topicList: JSONArray get() = JSON.parseArray(topic(mutableListOf("list" to null), null))
    val commandList: JSONArray get() = JSON.parseArray(run(mutableListOf("list" to null), null))


    fun runCommand(subCommands: List<String>?, flags: List<Pair<String, String?>>? = null, callback: ((String) -> Unit)? = null): String? {
        val cmd: MutableList<String> = mutableListOf(command)
        cmd.addAll(subCommands.orEmpty())
        flags?.forEach { (name, value) ->
            cmd.add("--$name")
            cmd.addIfNotNull(value)
        }
        return try {
            execCommand(cmd, callback)
        } catch (e: Exception) {
            Log.error("Failed to run command $cmd: ${e.message}")
            throw RuntimeException("Failed to run command $cmd", e)
        }
    }

    private fun subCommand(subCommands: List<String>): (MutableList<Pair<String, String?>>?, ((String) -> Unit)?) -> String? {
        val cmd: MutableList<String> = mutableListOf(command)
        cmd.addAll(subCommands)
        return {flags: List<Pair<String, String?>>?, callback: ((String) -> Unit)? ->
            flags?.forEach { (name, value) ->
                cmd.add("--$name")
                cmd.addIfNotNull(value)
            }
            try {
                execCommand(cmd, callback)
            } catch (e: Exception) {
                Log.error("Failed to run command $cmd: ${e.message}")
                throw RuntimeException("Failed to run command $cmd", e)
            }
        }
    }
}
