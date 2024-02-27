package ai.devchat.plugin

import ai.devchat.storage.DevChatState
import ai.devchat.storage.ToolWindowState
import ai.grazie.utils.applyIf
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener


class ToolWindowStateListener : ToolWindowManagerListener {
    override fun toolWindowsRegistered(ids: MutableList<String>, toolWindowManager: ToolWindowManager) {
        super.toolWindowsRegistered(ids, toolWindowManager)
        ids.find { it == "DevChat" }?.let {
            runInEdt {
                toolWindowManager.getToolWindow(it)?.applyIf(
                    DevChatState.instance.lastToolWindowState == ToolWindowState.SHOWN.name
                ) {
                    while (!this.isAvailable) {
                        Thread.sleep(1000)
                    }
                    this.show()
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
