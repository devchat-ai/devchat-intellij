package ai.devchat.cli

import ai.devchat.common.PathUtils
import ai.devchat.common.Log
import ai.devchat.common.Settings
import ai.devchat.idea.balloon.DevChatNotifier
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
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

class Command(val cmd: MutableList<String> = mutableListOf()) {
    private val env: MutableMap<String, String> = mutableMapOf()

    constructor(parent: Command) : this() {
        cmd.addAll(parent.cmd)
        env.putAll(parent.env)
    }

    fun addEnv(updates: Map<String, String>) {
        env.putAll(updates)
    }

    fun subcommand(subCmd: String): Command {
        cmd.add(subCmd)
        return this
    }

    fun exec(
        flags: List<Pair<String, String?>> = listOf(),
        callback: ((String) -> Unit)? = null,
        onFinish: ((Int) -> Unit)? = null
    ): String? {
        val args = flags.fold(mutableListOf<String>()) { acc, (name, value) ->
            acc.add("--$name")
            acc.addIfNotNull(value)
            acc
        }
        return try {
            callback?.let {
                execAsync(cmd + args, callback, DevChatNotifier::stickyError, onFinish);
                ""
            } ?: exec(cmd + args)
        } catch (e: Exception) {
            Log.warn("Failed to run command $cmd: ${e.message}")
            throw CommandExecutionException("Failed to run command $cmd: ${e.message}")
        }
    }

    private fun exec(commands: List<String>): String {
        Log.info("Executing command: ${commands.joinToString(" ")}}")
        return try {
            val outputLines: MutableList<String> = mutableListOf()
            val errorLines: MutableList<String> = mutableListOf()
            val exitCode = runBlocking {
                executeCommand(commands, env, outputLines::add, errorLines::add)
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

    private fun execAsync(
        commands: List<String>,
        onOutput: (String) -> Unit,
        onError: (String) -> Unit = Log::warn,
        onFinish: ((Int) -> Unit)? = null,
    ): Job {
        Log.info("Executing command: ${commands.joinToString(" ")}}")
        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            val msg = "Failed to execute command: $commands, Exception: $exception"
            Log.warn(msg)
            onError(msg)
        }
        val cmdScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        return cmdScope.launch(exceptionHandler) {
            val exitCode = executeCommand(commands, env, onOutput, onError)
            onFinish?.let { onFinish(exitCode) }
            if (exitCode != 0) {
                throw CommandExecutionException("Command failure with exit Code: $exitCode")
            }
        }
    }
}

class DevChatWrapper(
    private var apiBase: String? = null,
    private var apiKey: String? = null,
    private var defaultModel: String? = null
) {
    private val baseCommand = Command(mutableListOf(PathUtils.pythonCommand, "-m", "devchat"))

    init {
        if (apiBase.isNullOrEmpty() || apiKey.isNullOrEmpty() || defaultModel.isNullOrEmpty()) {
            val (key, api, model) = Settings.getAPISettings()
            apiBase = apiBase ?: api
            apiKey = apiKey ?: key
            defaultModel = defaultModel ?: model
        }
        baseCommand.addEnv(getEnv())
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
        env["PYTHONPATH"] = PathUtils.pythonPath
        return env
    }

    val runCmd = Command(baseCommand).subcommand("run")::exec
    val logCmd = Command(baseCommand).subcommand("log")::exec
    val topicCmd = Command(baseCommand).subcommand("topic")::exec
    val routeCmd = Command(baseCommand).subcommand("route")::exec

    val run get() = { flags: List<Pair<String, String?>>  ->  runCmd(flags, null, null)}
    val log get() = { flags: List<Pair<String, String?>>  ->  logCmd(flags, null, null)}
    val topic get() = { flags: List<Pair<String, String?>>  ->  topicCmd(flags, null, null)}

    fun route(
        flags: List<Pair<String, String?>>,
        message: String,
        callback: ((String) -> Unit)?,
        onFinish: ((Int) -> Unit)?
    ) {
        when {
            apiKey.isNullOrEmpty() -> DevChatNotifier.stickyError("Please config your API key first.")
            !apiKey!!.startsWith("DC.") -> DevChatNotifier.stickyError("Invalid API key format.")
            else -> routeCmd(
                flags
                        + (if (flags.any {
                            it.first == "model" && !it.second.isNullOrEmpty()
                        }) emptyList() else listOf("model" to defaultModel))
                        + listOf("" to message),
                callback,
                onFinish
            )
        }
    }

    val topicList: JSONArray get() = try {
        val r = topic(mutableListOf("list" to null)) ?: "[]"
        JSON.parseArray(r)
    } catch (e: Exception) {
        Log.warn("Error list topics: $e")
        JSONArray()
    }
    val commandList: JSONArray get() = try {
        val r = run(mutableListOf("list" to null)) ?: "[]"
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
            )) ?: "[]"
            JSON.parseArray(r)
        } catch (e: Exception) {
            Log.warn("Error log topic: $e")
            JSONArray()
        }
    }
}
