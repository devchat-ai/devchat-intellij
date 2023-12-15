package ai.devchat.cli

import ai.devchat.common.DevChatPathUtil
import ai.devchat.common.Log
import ai.devchat.common.Settings
import ai.devchat.idea.balloon.DevChatNotifier
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.intellij.util.alsoIfNull
import com.intellij.util.containers.addIfNotNull
import kotlinx.coroutines.*
import java.io.IOException

private const val DEFAULT_LOG_MAX_COUNT = 10000


class CommandExecutionException(message:String): Exception(message)
private suspend fun Process.await(
    onOutput: (String) -> Unit,
    onError: (String) -> Unit
): Int = coroutineScope {
    launch(Dispatchers.IO) {
        inputStream.bufferedReader().forEachLine { onOutput(it) }
        errorStream.bufferedReader().forEachLine { onError(it) }
    }
    val processExitCode = this@await.waitFor()
    processExitCode
}

suspend fun executeCommand(
    command: List<String>,
    env: Map<String, String>,
    onOutputLine: (String) -> Unit,
    onErrorLine: (String) -> Unit
): Int {
    val processBuilder = ProcessBuilder(command)
    env.forEach { (key, value) -> processBuilder.environment()[key] = value}
    val process = withContext(Dispatchers.IO) {
        processBuilder.start()
    }
    return process.await(onOutputLine, onErrorLine)
}

class DevChatWrapper(
    private val command: String = DevChatPathUtil.devchatBinPath,
    private var apiBase: String? = null,
    private var apiKey: String? = null,
    private var defaultModel: String? = null
) {
    init {
        if (apiBase.isNullOrEmpty() || apiKey.isNullOrEmpty() || defaultModel.isNullOrEmpty()) {
            val (key, api, model) = Settings.getAPISettings()
            apiBase = apiBase ?: api
            apiKey = apiKey ?: key
            defaultModel = defaultModel ?: model
        }
    }

    private fun getEnv(): Map<String, String> {
        val env: MutableMap<String, String> = mutableMapOf()
        apiBase?.let {
            env["OPENAI_API_BASE"] = it
            Log.info("api_base: $it")
        }
        apiKey?.let {
            env["OPENAI_API_KEY"] = it
            Log.info("api_key: ${it.substring(0, 5)}...${it.substring(it.length - 4)}")
        }
        return env
    }

    private fun execCommand(commands: List<String>): String {
        Log.info("Executing command: ${commands.joinToString(" ")}}")
        return try {
            val outputLines: MutableList<String> = mutableListOf()
            val errorLines: MutableList<String> = mutableListOf()
            val exitCode = runBlocking {
                executeCommand(commands, getEnv(), outputLines::add, errorLines::add)
            }
            val errors = errorLines.joinToString("\n")

            if (exitCode != 0) {
                throw CommandExecutionException("Command failure with exit Code: $exitCode, Errors: $errors")
            } else {
                outputLines.joinToString("\n")
            }
        } catch (e: IOException) {
            Log.warn("Failed to execute command: $commands, Exception: $e")
            throw e
        }
    }

    private fun execCommandAsync(
        commands: List<String>,
        onOutput: (String) -> Unit,
        onError: (String) -> Unit = Log::warn
    ): Job {
        Log.info("Executing command: ${commands.joinToString(" ")}}")
        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            Log.warn("Failed to execute command: $commands, Exception: $exception")
            onError("DevChat failed to response the message.")
        }
        val cmdScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        return cmdScope.launch(exceptionHandler) {
            val exitCode = executeCommand(commands, getEnv(), onOutput, onError)
            if (exitCode != 0) {
                throw CommandExecutionException("Command failure with exit Code: $exitCode")
            }
        }
    }

    val prompt: (MutableList<Pair<String, String?>>, String, ((String) -> Unit)?) -> Unit get() = {
        flags: MutableList<Pair<String, String?>>, message: String, callback: ((String) -> Unit)? ->
            when {
                apiKey.isNullOrEmpty() -> DevChatNotifier.stickyError("Please config your API key first.")
                !apiKey!!.startsWith("DC.") -> DevChatNotifier.stickyError("Invalid API key format.")
                else -> {
                    flags
                        .find { it.first == "model" && !it.second.isNullOrEmpty() }
                        .alsoIfNull { flags.add("model" to defaultModel) }
                    flags.add("" to message)
                    subCommand(listOf("prompt"))(flags, callback)
                }
            }
    }

    val run get() = subCommand(listOf("run"))
    val log get() = subCommand(listOf("log"))
    val topic get() = subCommand(listOf("topic"))

    val topicList: JSONArray get() = try {
        val r = topic(mutableListOf("list" to null), null) ?: "[]"
        JSON.parseArray(r)
    } catch (e: Exception) {
        Log.warn("Error list topics: $e")
        JSONArray()
    }
    val commandList: JSONArray get() = try {
        val r = run(mutableListOf("list" to null), null) ?: "[]"
        JSON.parseArray(r)
    } catch (e: Exception) {
        Log.warn("Error list commands: $e")
        JSONArray()
    }

    val logTopic: (String, Int?) -> JSONArray get() = {topic: String, maxCount: Int? ->
        val num: Int = maxCount ?: DEFAULT_LOG_MAX_COUNT
        try {
            val r = log(mutableListOf(
                "topic" to topic,
                "max-count" to num.toString()
            ), null) ?: "[]"
            JSON.parseArray(r)
        } catch (e: Exception) {
            Log.warn("Error log topic: $e")
            JSONArray()
        }
    }


    fun runCommand(subCommands: List<String>?, flags: List<Pair<String, String?>>? = null, callback: ((String) -> Unit)? = null): String? {
        val cmd: MutableList<String> = mutableListOf(command)
        cmd.addAll(subCommands.orEmpty())
        flags?.forEach { (name, value) ->
            cmd.add("--$name")
            cmd.addIfNotNull(value)
        }
        return try {
            callback?.let { execCommandAsync(cmd, callback, DevChatNotifier::stickyError); "" } ?: execCommand(cmd)
        } catch (e: Exception) {
            Log.warn("Failed to run command $cmd: ${e.message}")
            throw CommandExecutionException("Failed to run command $cmd, $e")
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
                callback?.let { execCommandAsync(cmd, callback, DevChatNotifier::stickyError); "" } ?: execCommand(cmd)
            } catch (e: Exception) {
                Log.warn("Failed to run command $cmd: ${e.message}")
                throw CommandExecutionException("Failed to run command $cmd: ${e.message}")
            }
        }
    }
}
