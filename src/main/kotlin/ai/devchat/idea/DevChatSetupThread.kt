package ai.devchat.idea

import ai.devchat.cli.DevChatConfig
import ai.devchat.cli.DevChatInstallationManager
import ai.devchat.common.DevChatPathUtil.workPath
import ai.devchat.common.Log.error
import ai.devchat.common.Log.info
import ai.devchat.idea.balloon.DevChatNotifier.notifyError
import ai.devchat.idea.balloon.DevChatNotifier.notifyInfo
import com.intellij.openapi.project.Project

class DevChatSetupThread(private val project: Project) : Thread() {
    override fun run() {
        val workPath = workPath
        info("Work path is: $workPath")
        notifyInfo(project, "Starting DevChat initialization...")
        try {
            val dim = DevChatInstallationManager(workPath, "0.2.9")
            dim.setup()
            val config = DevChatConfig()
            config.writeDefaultConfig()
            notifyInfo(project, "DevChat initialization has completed successfully.")
        } catch (e: Exception) {
            error("Failed to install DevChat CLI: " + e.message)
            notifyError(
                project,
                "DevChat initialization has failed. Please check the logs for more details."
            )
        }
    }
}
