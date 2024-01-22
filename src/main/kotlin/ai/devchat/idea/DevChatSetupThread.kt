package ai.devchat.idea

import ai.devchat.cli.DevChatConfig
import ai.devchat.cli.DevChatWrapper
import ai.devchat.cli.PythonEnvManager
import ai.devchat.common.ProjectUtils
import ai.devchat.common.Log
import ai.devchat.common.OSInfo
import ai.devchat.common.PathUtils
import ai.devchat.idea.balloon.DevChatNotifier
import ai.devchat.idea.settings.DevChatSettingsConfigurable
import ai.devchat.idea.settings.DevChatSettingsState
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
            ProjectUtils.executeJS("onInitializationFinish")
            DevChatNotifier.info("DevChat initialization has completed successfully.")
        } catch (e: Exception) {
            Log.error("Failed to install DevChat CLI: $e\n" + e.stackTrace.joinToString("\n"))
            DevChatNotifier.error("DevChat initialization has failed. Please check the logs for more details.")
        }
    }

    private fun setupDevChat(envManager: PythonEnvManager) {
        val sitePackagePath = PathUtils.copyResourceDirToPath(
            "/tools/site-packages",
            Paths.get(workDir, "site-packages").toString()
        )

        DevChatSettingsState.instance.pythonForChat = getSystemPython(minimalPythonVersion) ?: (
            if (OSInfo.isWindows) {
                val basePath = Paths.get(workDir, "python-win").toString()
                PathUtils.copyResourceDirToPath("/tools/python-3.11.6-embed-amd64", basePath)
                val pthFile = File(Paths.get(basePath, "python311._pth").toString())
                val pthContent = pthFile.readText().replace("%PYTHONPATH%", sitePackagePath)
                pthFile.writeText(pthContent)
                Paths.get(basePath, "python.exe").toString()
            }
            else envManager.createEnv(
                "devchat", defaultPythonVersion
            ).pythonCommand
        )
        DevChatSettingsConfigurable.get().reset()
        DevChatConfig(Paths.get(workDir, "config.yml").toString()).writeDefaultConfig()
    }

    private fun setupWorkflows(envManager: PythonEnvManager) {
        val workflowsDir = File(Paths.get(workDir, "workflows").toString())
        if (!workflowsDir.exists()) workflowsDir.mkdirs()
        PathUtils.copyResourceDirToPath(
            "/workflows",
            Paths.get(workflowsDir.path, "sys").toString()
        )
        try {
            DevChatWrapper().run(mutableListOf("update-sys" to null))
        } catch (e: Exception) {
            Log.warn("Failed to update-sys.")
        }
        listOf("sys", "org", "usr")
            .map { Paths.get(workflowsDir.path, it, "requirements.txt").toString() }
            .firstOrNull { File(it).exists() }
            ?.let {
                val workflowEnv = envManager.createEnv("devchat-commands", defaultPythonVersion)
                workflowEnv.installRequirements(it)
                PathUtils.pythonForWorkflows = workflowEnv.pythonCommand
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

        return output?.let {
            val (major, minor) = it.split(" ")[1].split(".").take(2).map(String::toInt)
            val cmd = "import sys; print(sys.executable)"
            val proc = ProcessBuilder(
                if (OSInfo.isWindows) listOf("cmd","/c","python -c \"$cmd\"")
                else listOf("/bin/bash","-c", "python3 -c \"$cmd\"")
            ).start()
            val python = proc.inputStream.bufferedReader().use(BufferedReader::readLine).trim()
            proc.waitFor()
            when {
                major > minMajor -> python
                major == minMajor && minor >= minMinor -> python
                else -> null
            }
        }
    }
}
