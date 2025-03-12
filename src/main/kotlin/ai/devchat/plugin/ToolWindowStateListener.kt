package ai.devchat.plugin

import ai.devchat.common.Constants.ASSISTANT_NAME_ZH
import ai.devchat.common.DevChatBundle
import ai.devchat.storage.DevChatState
import ai.devchat.storage.ToolWindowState
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener


class ToolWindowStateListener : ToolWindowManagerListener {
    override fun toolWindowsRegistered(ids: MutableList<String>, toolWindowManager: ToolWindowManager) {
        super.toolWindowsRegistered(ids, toolWindowManager)
        ids.find { it == ASSISTANT_NAME_ZH }?.let {
            runInEdt {
                val toolWindow = toolWindowManager.getToolWindow(it)
                if (DevChatState.instance.lastToolWindowState == ToolWindowState.SHOWN.name && toolWindow != null) {
                    while (!toolWindow.isAvailable) {
                        Thread.sleep(1000)
                    }
                    toolWindow.show()
                }
            }
        }
    }

    override fun stateChanged(
        toolWindowManager: ToolWindowManager,
        changeType: ToolWindowManagerListener.ToolWindowManagerEventType
    ) {
        super.stateChanged(toolWindowManager, changeType)
        when (changeType) {
            ToolWindowManagerListener.ToolWindowManagerEventType.ActivateToolWindow -> {
                val jsFocus = "document.getElementsByClassName('mantine-Input-input mantine-Textarea-input')[0].focus();"
                toolWindowManager.getToolWindow(DevChatBundle.message("plugin.id"))?.project?.let { project ->
                    project.getService(DevChatService::class.java).browser?.jbCefBrowser?.cefBrowser?.executeJavaScript(jsFocus, "", 0)
                }
                DevChatState.instance.lastToolWindowState = ToolWindowState.SHOWN.name
            }
            ToolWindowManagerListener.ToolWindowManagerEventType.HideToolWindow -> {
                DevChatState.instance.lastToolWindowState = ToolWindowState.HIDDEN.name
            }
            else -> {}
        }
    }
}
