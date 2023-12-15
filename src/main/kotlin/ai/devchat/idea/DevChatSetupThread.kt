package ai.devchat.idea

import ai.devchat.cli.DevChatConfig
import ai.devchat.cli.DevChatWrapper
import ai.devchat.cli.PythonEnvManager
import ai.devchat.common.DevChatPathUtil.workPath
import ai.devchat.common.Log
import ai.devchat.idea.balloon.DevChatNotifier.notifyError
import ai.devchat.idea.balloon.DevChatNotifier.notifyInfo
import com.intellij.openapi.project.Project
import java.io.File

class DevChatSetupThread(private val project: Project) : Thread() {
    override fun run() {
        val workPath = workPath
        Log.info("Work path is: $workPath")
        notifyInfo(project, "Starting DevChat initialization...")
        try {
            Log.info("Start configuring the DevChat CLI environment.")
            val envManager = PythonEnvManager(workPath)
            val devChatEnv = envManager.createEnv("devchat", "3.11.4")
            devChatEnv.installPackage("devchat", "0.2.10")
            try {
                DevChatWrapper().run(mutableListOf("update-sys" to null), null)
            } catch (e: Exception) {
                Log.warn("Failed to update-sys.")
            }
            listOf("sys", "org", "usr")
                .map { "$workPath/workflows/$it/requirements.txt" }
                .firstOrNull { File(it).exists() }
                ?.let {
                    val workflowEnv = envManager.createEnv("devchat-commands", "3.11.4")
                    workflowEnv.installRequirements(it)
                }

            val config = DevChatConfig("$workPath/config.yml")
            config.writeDefaultConfig()
            notifyInfo(project, "DevChat initialization has completed successfully.")
        } catch (e: Exception) {
            Log.error("Failed to install DevChat CLI: " + e.message)
            notifyError(
                project,
                "DevChat initialization has failed. Please check the logs for more details."
            )
        }
    }
}
