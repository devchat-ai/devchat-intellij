package ai.devchat.cli

import ai.devchat.common.PathUtils
import ai.devchat.common.Log
import ai.devchat.common.ProjectUtils
import ai.devchat.common.Settings
import ai.devchat.idea.balloon.DevChatNotifier
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.intellij.util.containers.addIfNotNull
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.selects.whileSelect
import java.io.File
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
    workDir: String?,
    env: Map<String, String>,
): Process {
    val processBuilder = ProcessBuilder(command)
    workDir?.let {processBuilder.directory(File(workDir))}
    env.forEach { (key, value) -> processBuilder.environment()[key] = value}
    return withContext(Dispatchers.IO) {
        processBuilder.start()
    }
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

    private fun prepare(flags: List<Pair<String, String?>> = listOf()): List<String> {
        val args = flags.fold(mutableListOf<String>()) { acc, (name, value) ->
            acc.add("--$name")
            acc.addIfNotNull(value)
            acc
        }
        return cmd + args
    }

    private fun toString(flags: List<Pair<String, String?>>): String {
        val preparedCommand = prepare(flags)
        return env.entries.joinToString(" ") { (k, v) ->
            val masked = if (k == "OPENAI_API_KEY") v.mapIndexed { i, c ->
                if (i in 7 until v.length - 7) '*' else c
            }.joinToString("") else v
            "$k=$masked"
        } + " " + preparedCommand.joinToString(" ")
    }

    fun exec(flags: List<Pair<String, String?>> = listOf()): String {
        val preparedCommand = prepare(flags)
        val commandStr = toString(flags)
        Log.info("Executing command: $commandStr")
        return try {
            val outputLines: MutableList<String> = mutableListOf()
            val errorLines: MutableList<String> = mutableListOf()
            val exitCode = runBlocking {
                executeCommand(
                    preparedCommand,
                    ProjectUtils.project?.basePath,
                    env
                ).await(outputLines::add, errorLines::add)
            }
            val errors = errorLines.joinToString("\n")

            if (exitCode != 0) {
                throw CommandExecutionException("Command failure with exit Code: $exitCode, Errors: $errors")
            } else {
                outputLines.joinToString("\n")
            }
        } catch (e: Exception) {
            val msg = "Failed to execute command `$commandStr`: $e"
            Log.warn(msg)
            throw CommandExecutionException(msg)
        }
    }

    @OptIn(ObsoleteCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    fun execAsync(
        flags: List<Pair<String, String?>>,
        onOutput: (String) -> Unit,
        onError: (String) -> Unit = Log::warn,
        onFinish: ((Int) -> Unit)? = null,
    ): SendChannel<String> {
        val preparedCommand = prepare(flags)
        val commandStr = toString(flags)
        Log.info("Executing command: $commandStr")
        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            val msg = "Failed to execute command `$commandStr`: $exception"
            Log.warn(msg)
            onError(msg)
        }
        return CoroutineScope(
            SupervisorJob() + Dispatchers.Default + exceptionHandler
        ).actor {
            val process = executeCommand(preparedCommand, ProjectUtils.project?.basePath, env)
            val writer = process.outputStream.bufferedWriter()
            val deferred = async {process.await(onOutput, onError)}
            var exitCode = 0
            whileSelect {
                deferred.onAwait {
                    writer.close()
                    exitCode = it
                    false
                }
                channel.onReceive {
                    writer.write(it)
                    writer.flush()
                    Log.info("Input wrote: $it")
                    true
                }
            }
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
            env["OPENAI_BASE_URL"] = it
            Log.info("api_base: $it")
        }
        apiKey?.let {
            env["OPENAI_API_KEY"] = it
            Log.info("api_key: ${it.substring(0, 5)}...${it.substring(it.length - 4)}")
        }
        env["PYTHONPATH"] = PathUtils.pythonPath
        env["command_python"] = PathUtils.pythonForWorkflows
        env["DEVCHAT_IDE_SERVICE_URL"] = "http://localhost:${ProjectUtils.ideServerPort}"
        env["DEVCHAT_IDE_SERVICE_PORT"] = ProjectUtils.ideServerPort.toString()
        return env
    }

    val run = Command(baseCommand).subcommand("run")::exec
    val log = Command(baseCommand).subcommand("log")::exec
    val topic = Command(baseCommand).subcommand("topic")::exec
    val routeCmd = Command(baseCommand).subcommand("route")::execAsync

    fun route(
        flags: List<Pair<String, String?>>,
        message: String,
        callback: (String) -> Unit,
        onError: (String) -> Unit = DevChatNotifier::stickyError,
        onFinish: ((Int) -> Unit)? = null
    ) {
        when {
            apiKey.isNullOrEmpty() -> DevChatNotifier.stickyError("Please config your API key first.")
            !apiKey!!.startsWith("DC.") -> DevChatNotifier.stickyError("Invalid API key format.")
            else -> activeChannel = routeCmd(
                flags
                        + (if (flags.any {
                            it.first == "model" && !it.second.isNullOrEmpty()
                        }) emptyList() else listOf("model" to defaultModel))
                        + listOf("" to message),
                callback,
                onError,
                onFinish
            )
        }
    }

    val topicList: JSONArray get() = try {
        val r = topic(mutableListOf("list" to null))
        JSON.parseArray(r)
    } catch (e: Exception) {
        Log.warn("Error list topics: $e")
        JSONArray()
    }
    val commandList: JSONArray get() = try {
        JSON.parseArray(run(mutableListOf("list" to null)))
    } catch (e: Exception) {
        Log.warn("Error list commands: $e")
        JSONArray()
    }

    val logTopic: (String, Int?) -> JSONArray get() = {topic: String, maxCount: Int? ->
        val num: Int = maxCount ?: DEFAULT_LOG_MAX_COUNT
        try {
            JSON.parseArray(log(mutableListOf(
                "topic" to topic,
                "max-count" to num.toString()
            )))
        } catch (e: Exception) {
            Log.warn("Error log topic: $e")
            JSONArray()
        }
    }
    val logInsert: (String) -> Unit get() = { item: String ->
        try {
            log(listOf("insert" to item))
        } catch (e: Exception) {
            Log.warn("Error insert log: $e")
        }
    }

    val logLast: () -> JSONObject? get() = {
        try {
            log(mutableListOf(
                "max-count" to "1"
            )).let {
                JSON.parseArray(it).getJSONObject(0)
            }
        } catch (e: Exception) {
            Log.warn("Error log topic: $e")
            null
        }
    }
    companion object {
        var activeChannel: SendChannel<String>? = null
    }

}
