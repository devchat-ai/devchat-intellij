package ai.devchat.idea

import ai.devchat.idea.storage.DevChatState
import ai.devchat.idea.storage.ToolWindowState
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener


class ToolWindowStateListener : ToolWindowManagerListener {
    override fun toolWindowsRegistered(ids: MutableList<String>, toolWindowManager: ToolWindowManager) {
        super.toolWindowsRegistered(ids, toolWindowManager)
        ids.find { it == "DevChat" }?.let {
            runInEdt {
                toolWindowManager.getToolWindow(it)?.let {
                    if (DevChatState.instance.lastToolWindowState == ToolWindowState.SHOWN.name) {
                        while (!it.isAvailable) {
                            Thread.sleep(1000)
                        }
                        it.show()
                    }
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
            ToolWindowManagerListener.ToolWindowManagerEventType.ShowToolWindow -> {
                DevChatState.instance.lastToolWindowState = ToolWindowState.SHOWN.name
            }
            ToolWindowManagerListener.ToolWindowManagerEventType.HideToolWindow -> {
                DevChatState.instance.lastToolWindowState = ToolWindowState.HIDDEN.name
            }
            else -> {}
        }
    }
}
