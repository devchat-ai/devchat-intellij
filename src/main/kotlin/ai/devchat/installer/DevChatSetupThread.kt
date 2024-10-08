package ai.devchat.installer

import ai.devchat.common.*
import ai.devchat.common.Constants.ASSISTANT_NAME_EN
import ai.devchat.core.DevChatClient
import ai.devchat.plugin.DevChatService
import ai.devchat.storage.CONFIG
import ai.devchat.storage.DevChatState
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import java.io.BufferedReader
import java.io.File
import java.nio.file.Paths
import kotlin.system.measureTimeMillis

class DevChatSetupThread(val project: Project) : Thread() {
    private val minimalPythonVersion: String = "3.8"
    private val defaultPythonVersion: String = "3.11.4"
    private val devChatService = project.getService(DevChatService::class.java)
    private val devChatVersion = PluginManagerCore.getPlugin(
        PluginId.getId(DevChatBundle.message("plugin.id"))
    )?.version

    override fun run() {
        Log.info("Work path is: ${PathUtils.workPath}")
        Notifier.info("Starting $ASSISTANT_NAME_EN initialization...")
        try {
            Log.info("Start configuring the $ASSISTANT_NAME_EN CLI environment.")
            val executionTime = measureTimeMillis {
                setupPython(PythonEnvManager())
            }
            Log.info("-----------> Time took to setup python: ${executionTime/1000} s")
            installTools()
            updateWorkflows()
            devChatService.browser?.let {
                Log.info("-----------> Executing JS callback onInitializationFinish")
                it.executeJS("onInitializationFinish")
            }
            DevChatState.instance.lastVersion = devChatVersion
            Notifier.info("$ASSISTANT_NAME_EN initialization has completed successfully.")
        } catch (e: Exception) {
            Log.error("Failed to install $ASSISTANT_NAME_EN CLI: $e\n" + e.stackTrace.joinToString("\n"))
            Notifier.error("$ASSISTANT_NAME_EN initialization has failed. Please check the logs for more details.")
        }
    }

    private fun setupPython(envManager: PythonEnvManager) {
        val overwrite = devChatVersion != DevChatState.instance.lastVersion
        PathUtils.copyResourceDirToPath("/tools/site-packages", PathUtils.sitePackagePath, overwrite)
        "python_for_chat".let { k ->
            if (OSInfo.isWindows) {
                val installDir = Paths.get(PathUtils.workPath, "python-win").toString()
                PathUtils.copyResourceDirToPath("/tools/python-3.11.6-embed-amd64", installDir, overwrite)
                val pthFile = File(Paths.get(installDir, "python311._pth").toString())
                val pthContent = pthFile.readText().replace(
                    "%PYTHONPATH%",
                    "${PathUtils.sitePackagePath}${System.lineSeparator()}${PathUtils.workflowPath}"
                )
                pthFile.writeText(pthContent)
                CONFIG[k] = Paths.get(installDir, "python.exe").toString()
            } else if ((CONFIG[k] as? String).isNullOrEmpty()) {
                CONFIG[k] = getSystemPython(minimalPythonVersion) ?: envManager.createEnv(
                    "devchat", defaultPythonVersion
                ).pythonCommand
            }
        }
        devChatService.pythonReady = true
    }

    private fun installTools() {
        val overwrite = devChatVersion != DevChatState.instance.lastVersion
        PathUtils.copyResourceDirToPath(
            "/tools/code-editor/${PathUtils.codeEditorBinary}",
            Paths.get(PathUtils.toolsPath, PathUtils.codeEditorBinary).toString(),
            overwrite
        )
        PathUtils.copyResourceDirToPath(
            "/tools/sonar-rspec",
            Paths.get(PathUtils.toolsPath, "sonar-rspec").toString(),
            overwrite
        )
        PathUtils.copyResourceDirToPath("/workflows", PathUtils.workflowPath)
    }

    private fun updateWorkflows() {
        try {
            val dcClient: DevChatClient = devChatService.client!!
            dcClient.updateWorkflows()
            dcClient.updateCustomWorkflows()
        } catch (e: Exception) {
            Log.warn("Failed to update workflows: $e")
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
            val pb = ProcessBuilder(
                if (OSInfo.isWindows) listOf("cmd","/c","python -c \"$cmd\"")
                else listOf("/bin/bash","-c", "python3 -c \"$cmd\"")
            )
            pb.environment()["PYTHONUTF8"] = "1"
            val proc = pb.start()
            val python = proc.inputStream.bufferedReader().use(BufferedReader::readText).trim()
            val errs = proc.errorStream.bufferedReader().use(BufferedReader::readText)
            val exitCode = proc.waitFor()
            if (exitCode != 0) {
                Log.warn("Failed to get system: $errs")
            }
            when {
                major > minMajor -> python
                major == minMajor && minor >= minMinor -> python
                else -> null
            }
        }
    }
}
