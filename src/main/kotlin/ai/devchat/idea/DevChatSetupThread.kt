package ai.devchat.idea

import ai.devchat.cli.DevChatConfig
import ai.devchat.cli.DevChatWrapper
import ai.devchat.cli.PythonEnvManager
import ai.devchat.common.Log
import ai.devchat.common.OSInfo
import ai.devchat.common.PathUtils
import ai.devchat.idea.balloon.DevChatNotifier
import java.io.BufferedReader
import java.io.File
import java.nio.file.Paths

class DevChatSetupThread : Thread() {

    private val minimalPythonVersion: String = "3.8"
    private val defaultPythonVersion: String = "3.11.4"
    private val workDir = PathUtils.workPath

    override fun run() {
        Log.info("Work path is: $workDir")
        DevChatNotifier.info("Starting DevChat initialization...")
        try {
            Log.info("Start configuring the DevChat CLI environment.")
            val envManager = PythonEnvManager(workDir)
            setupDevChat(envManager)
            setupWorkflows(envManager)
            DevChatNotifier.info("DevChat initialization has completed successfully.")
        } catch (e: Exception) {
            Log.error("Failed to install DevChat CLI: " + e.message)
            DevChatNotifier.error("DevChat initialization has failed. Please check the logs for more details.")
        }
    }

    private fun setupDevChat(envManager: PythonEnvManager) {
        PathUtils.copyResourceDirToPath(
            "/tools/site-packages",
            Paths.get(workDir, "site-packages").toString()
        )

        PathUtils.pythonCommand = getSystemPython(minimalPythonVersion) ?: envManager.createEnv(
            "devchat", defaultPythonVersion
        ).pythonCommand
        DevChatConfig(Paths.get(workDir, "config.yml").toString()).writeDefaultConfig()
    }

    private fun setupWorkflows(envManager: PythonEnvManager) {
        try {
            DevChatWrapper().run(mutableListOf("update-sys" to null), null)
        } catch (e: Exception) {
            Log.warn("Failed to update-sys.")
        }
        listOf("sys", "org", "usr")
            .map { Paths.get(workDir, "workflows", it, "requirements.txt").toString() }
            .firstOrNull { File(it).exists() }
            ?.let {
                val workflowEnv = envManager.createEnv("devchat-commands", defaultPythonVersion)
                workflowEnv.installRequirements(it)
            }
    }

    private fun getSystemPython(minimalVersion: String): String? {
        val (minMajor, minMinor) = minimalVersion.split(".").take(2).map(String::toInt)
        val process = ProcessBuilder(
            if (OSInfo.isWindows) listOf("cmd","/c","python --version")
            else listOf("/bin/bash","-c", "python3 --version")
        ).start()
        val output = process.inputStream.bufferedReader().use(BufferedReader::readLine)
        process.waitFor()
        val python = if (OSInfo.isWindows) "python" else "python3"

        return output?.let {
            val (major, minor) = it.split(" ")[1].split(".").take(2).map(String::toInt)
            when {
                major > minMajor -> python
                major == minMajor && minor >= minMinor -> python
                else -> null
            }
        }
    }
}
