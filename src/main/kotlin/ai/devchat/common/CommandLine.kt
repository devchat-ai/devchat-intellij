package ai.devchat.common

import java.io.BufferedReader


data class CommandResult(val output: String, val errors: String, val exitCode: Int)

object CommandLine {
   fun exec(vararg command: String): CommandResult {
        val process = ProcessBuilder(*command).start()
        val output = process.inputStream.bufferedReader(charset=Charsets.UTF_8).use(BufferedReader::readText)
        val errors = process.errorStream.bufferedReader(charset=Charsets.UTF_8).use(BufferedReader::readText)
        val exitCode = process.waitFor()
        return CommandResult(output, errors, exitCode)
    }
}