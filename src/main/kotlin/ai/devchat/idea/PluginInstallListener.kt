package ai.devchat.idea

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.ToolWindowManager

class PluginInstallListener : DynamicPluginListener {
    override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
        if (pluginDescriptor.pluginId.idString == "ai.devchat.plugin") {
            val openProjects = ProjectManager.getInstance().openProjects
            if (openProjects.isNotEmpty()) {
                ToolWindowManager.getInstance(openProjects[0]).getToolWindow("DevChat")?.show()
            }
        }
    }
}