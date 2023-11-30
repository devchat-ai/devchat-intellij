package ai.devchat.cli

import ai.devchat.common.Log
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Consumer

class DevChatWrapper {
    private var apiBase: String? = null
    private var apiKey: String? = null
    private var currentModel: String? = null
    private var command: String
    private val DEFAULT_LOG_MAX_COUNT = "100"

    constructor(command: String) {
        this.command = command
    }

    constructor(apiBase: String?, apiKey: String?, currentModel: String?, command: String) {
        this.apiBase = apiBase
        this.apiKey = apiKey
        this.currentModel = currentModel
        this.command = command
    }

    private fun execCommand(commands: List<String?>): String {
        val pb = ProcessBuilder(commands)
        if (apiBase != null) {
            pb.environment()["OPENAI_API_BASE"] = apiBase
            Log.info("api_base: $apiBase")
        }
        if (apiKey != null) {
            pb.environment()["OPENAI_API_KEY"] = apiKey
            Log.info(
                "api_key: " + apiKey!!.substring(0, 5) + "..."
                        + apiKey!!.substring(apiKey!!.length - 4, apiKey!!.length)
            )
        }
        return try {
            Log.info("Executing command: " + java.lang.String.join(" ", pb.command()))
            val process = pb.start()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val error = readOutput(process.errorStream)
                Log.error("Failed to execute command: $commands Exit Code: $exitCode Error: $error")
                throw RuntimeException(
                    "Failed to execute command: $commands Exit Code: $exitCode Error: $error"
                )
            }
            readOutput(process.inputStream)
        } catch (e: IOException) {
            Log.error("Failed to execute command: $commands")
            throw RuntimeException("Failed to execute command: $commands", e)
        } catch (e: InterruptedException) {
            Log.error("Failed to execute command: $commands")
            throw RuntimeException("Failed to execute command: $commands", e)
        }
    }

    private fun execCommand(commands: List<String?>, callback: Consumer<String>) {
        val pb = ProcessBuilder(commands)
        if (apiBase != null) {
            pb.environment()["OPENAI_API_BASE"] = apiBase
            Log.info("api_base: $apiBase")
        }
        if (apiKey != null) {
            pb.environment()["OPENAI_API_KEY"] = apiKey
            Log.info(
                "api_key: " + apiKey!!.substring(0, 5) + "..."
                        + apiKey!!.substring(apiKey!!.length - 4, apiKey!!.length)
            )
        }
        try {
            Log.info("Executing command: " + java.lang.String.join(" ", pb.command()))
            val process = pb.start()
            readOutputByLine(process.inputStream, callback)
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val error = readOutput(process.errorStream)
                Log.error(
                    "Failed to execute command: " + java.lang.String.join(" ", pb.command()) + " Exit Code: " + exitCode
                            + " Error: " + error
                )
                throw RuntimeException(
                    "Failed to execute command: " + java.lang.String.join(" ", pb.command()) + " Exit Code: " + exitCode
                            + " Error: " + error
                )
            }
        } catch (e: IOException) {
            Log.error("Failed to execute command: " + java.lang.String.join(" ", pb.command()))
            throw RuntimeException("Failed to execute command: " + java.lang.String.join(" ", pb.command()), e)
        } catch (e: InterruptedException) {
            Log.error("Failed to execute command: " + java.lang.String.join(" ", pb.command()))
            throw RuntimeException("Failed to execute command: " + java.lang.String.join(" ", pb.command()), e)
        }
    }

    @Throws(IOException::class)
    private fun readOutput(inputStream: InputStream): String {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val output = StringBuilder()
        reader.forEachLine { line ->
            output.append(line)
            output.append('\n')
        }
        return output.toString()
    }

    @Throws(IOException::class)
    private fun readOutputByLine(inputStream: InputStream, callback: Consumer<String>) {
        val reader = BufferedReader(InputStreamReader(inputStream))
        reader.forEachLine { line -> callback.accept(line) }
    }

    fun runPromptCommand(flags: MutableMap<String, List<String?>>, message: String?, callback: Consumer<String>) {
        try {
            flags["model"] = listOf(currentModel)
            val commands = prepareCommand("prompt", flags, message)
            execCommand(commands, callback)
        } catch (e: Exception) {
            throw RuntimeException("Fail to run [prompt] command", e)
        }
    }

    fun runLogCommand(flags: Map<String, List<String?>>?): String {
        return try {
            val commands: List<String?> = prepareCommand(flags, "log")
            execCommand(commands)
        } catch (e: Exception) {
            throw RuntimeException("Failed to run [log] command", e)
        }
    }

    val commandList: JSONArray
        get() {
            val result = runCommand(null, "run", "--list")
            return JSON.parseArray(result)
        }
    val commandNamesList: Array<String?>
        get() {
            val commandList = commandList
            val names = arrayOfNulls<String>(commandList.size)
            for (i in commandList.indices) {
                names[i] = commandList.getJSONObject(i).getString("name")
            }
            return names
        }

    fun listConversationsInOneTopic(topicHash: String): JSONArray {
        val result = runLogCommand(
            java.util.Map.of<String, List<String?>>(
                "topic", listOf(topicHash),
                "max-count", listOf(DEFAULT_LOG_MAX_COUNT)
            )
        )
        return JSON.parseArray(result)
    }

    fun listTopics(): JSONArray {
        val result = runCommand(null, "topic", "--list")
        return JSON.parseArray(result)
    }

    fun runCommand(flags: Map<String, List<String?>>?, vararg subCommands: String): String {
        return try {
            val commands: List<String?> = prepareCommand(flags, *subCommands)
            execCommand(commands)
        } catch (e: Exception) {
            Log.error("Failed to run [run] command: " + e.message)
            throw RuntimeException("Failed to run [${subCommands}] command", e)
        }
    }

    private fun prepareCommand(flags: Map<String, List<String?>>?, vararg subCommands: String): MutableList<String?> {
        val commands: MutableList<String?> = ArrayList()
        commands.add(command)
        Collections.addAll(commands, *subCommands)
        if (flags == null) {
            return commands
        }
        flags.forEach(BiConsumer { flag: String, values: List<String?> ->
            for (value in values) {
                commands.add("--$flag")
                commands.add(value)
            }
        })
        return commands
    }

    private fun prepareCommand(subCommand: String, flags: Map<String, List<String?>>, message: String?): List<String?> {
        val commands = prepareCommand(flags, subCommand)
        // Add the message to the command list
        if (!message.isNullOrEmpty()) {
            commands.add("--")
            commands.add(message)
        }
        return commands
    }
}
