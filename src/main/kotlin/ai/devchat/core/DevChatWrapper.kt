package ai.devchat.core

import ai.devchat.common.Log
import ai.devchat.common.Notifier
import ai.devchat.common.PathUtils
import ai.devchat.plugin.currentProject
import ai.devchat.plugin.ideServerPort
import ai.devchat.storage.CONFIG
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.intellij.util.containers.addIfNotNull
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
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
        try {
            inputStream.bufferedReader(charset=Charsets.UTF_8).forEachLine { onOutput(it) }
            errorStream.bufferedReader(charset=Charsets.UTF_8).forEachLine { onError(it) }
        } catch (ex: IOException) {
            Log.warn("Stream is closed")
        }
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

    fun addEnv(updates: Map<String, String>): Command {
        env.putAll(updates)
        return this
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
        val startTime = System.currentTimeMillis()
        return try {
            val outputLines: MutableList<String> = mutableListOf()
            val errorLines: MutableList<String> = mutableListOf()
            val exitCode = runBlocking {
                executeCommand(
                    preparedCommand,
                    currentProject?.basePath,
                    env
                ).await(outputLines::add, errorLines::add)
            }
            val errors = errorLines.joinToString("\n")
            val outputs = outputLines.joinToString("\n")

            val endTime = System.currentTimeMillis()
            Log.info("Execution time: ${endTime - startTime} ms")

            if (exitCode != 0) {
                throw CommandExecutionException("Command failure with exit Code: $exitCode, Errors: $errors, Outputs: $outputs")
            } else {
                outputs
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
        val startTime = System.currentTimeMillis()
        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            Log.warn("Failed to execute command `$commandStr`: $exception")
            exception.printStackTrace()
            val msg = if (exception is NullPointerException) {
                "The current system environment is a bit abnormal, please try again later."
            } else exception.toString()
            onError("Failed to execute devchat command: $msg")
        }
        return CoroutineScope(
            SupervisorJob() + Dispatchers.Default + exceptionHandler
        ).actor {
            val process = executeCommand(preparedCommand, currentProject?.basePath, env)
            val writer = process.outputStream.bufferedWriter()
            val errorLines: MutableList<String> = mutableListOf()
            val deferred = async {process.await(onOutput, errorLines::add)}
            var exitCode = 0
            var cancelled = false
            whileSelect {
                deferred.onAwait {
                    writer.close()
                    exitCode = it
                    false
                }
                channel.onReceiveCatching {cr ->
                    if (cr.isClosed) {
                        Log.info("Channel was closed")
                        writer.close()
                        if (process.isAlive) killProcessTree(process)
                        cancelled = true
                        false
                    } else {
                        cr.getOrNull()?.let {
                            writer.write(it)
                            writer.flush()
                            onOutput(it)
                            Log.info("Input wrote: $it")
                        }
                        true
                    }
                }
            }
            val endTime = System.currentTimeMillis()
            Log.info("Execution time: ${endTime - startTime} ms")
            if (cancelled) return@actor
            val err = errorLines.joinToString("\n")
            if (exitCode != 0) {
                throw CommandExecutionException("Command failure with exit Code: $exitCode, errors: $err")
            }
            if (errorLines.isNotEmpty()) {
                Log.warn(err)
            }
            onFinish?.let { onFinish(exitCode) }
        }
    }
}

private fun killProcessTree(process: Process) {
    val pid = process.pid()  // Get the PID of the process
    ProcessHandle.of(pid).ifPresent { handle ->
        handle.descendants().forEach { descendant ->
            descendant.destroy()  // Attempt graceful shutdown
            descendant.destroyForcibly() // Force shutdown if necessary
        }
        handle.destroy()
        handle.destroyForcibly()
    }
}


class DevChatWrapper(
) {
    private val apiKey get() = CONFIG["providers.devchat.api_key"] as? String
    private val defaultModel get() = CONFIG["default_model"] as? String

    private val apiBase: String?
        get() {
            val k = "providers.devchat.api_base"
            var v = CONFIG[k] as? String
            if (v.isNullOrEmpty()) {
                v = when {
                    apiKey?.startsWith("sk-") == true -> "https://api.openai.com/v1"
                    apiKey?.startsWith("DC.") == true -> "https://api.devchat.ai/v1"
                    else -> v
                }
                CONFIG[k] = v
            }
            return v
        }
    private val baseCommand get() = Command(
        mutableListOf(CONFIG["python_for_chat"] as String, "-m", "devchat")
    ).addEnv(getEnv())

    private fun getEnv(): Map<String, String> {
        val env: MutableMap<String, String> = mutableMapOf()
        apiBase?.let {
            env["OPENAI_API_BASE"] = it
            env["OPENAI_BASE_URL"] = it
        }
        apiKey?.let {
            env["OPENAI_API_KEY"] = it
        }
        env["PYTHONPATH"] = PathUtils.pythonPath
        env["DEVCHAT_IDE_SERVICE_URL"] = "http://localhost:${ideServerPort}"
        env["DEVCHAT_IDE_SERVICE_PORT"] = ideServerPort.toString()
        env["PYTHONUTF8"] = "1"
        env["DEVCHAT_UNIT_TESTS_USE_USER_MODEL"] = "1"
        env["MAMBA_BIN_PATH"] = PathUtils.mambaBinPath
        return env
    }

    val topic get() = Command(baseCommand).subcommand("topic")::exec
    val routeCmd get() = Command(baseCommand).subcommand("route")::execAsync

    fun route(
        flags: List<Pair<String, String?>>,
        message: String,
        callback: (String) -> Unit,
        onError: (String) -> Unit = Notifier::stickyError,
        onFinish: ((Int) -> Unit)? = null
    ) {
        if (apiKey.isNullOrEmpty()) {
            onError("Please config your API key in DevChat settings first.")
            return
        }
        var additionalFlags = listOf("" to message)
        val modelConfigured = flags.any { it.first == "model" && !it.second.isNullOrEmpty() }
        if (!modelConfigured) additionalFlags = listOf("model" to defaultModel!!) + additionalFlags
        activeChannel?.close()
        activeChannel = routeCmd(flags + additionalFlags, callback, onError, onFinish)
    }

    val topicList: JSONArray get() = try {
        val r = topic(mutableListOf("list" to null))
        JSON.parseArray(r)
    } catch (e: Exception) {
        Log.warn("Error list topics: $e")
        JSONArray()
    }

    companion object {
        var activeChannel: SendChannel<String>? = null
    }

}
