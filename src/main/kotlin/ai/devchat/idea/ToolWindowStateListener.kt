package ai.devchat.idea

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener


class ToolWindowStateListener : ToolWindowManagerListener {
    override fun stateChanged(
        toolWindowManager: ToolWindowManager,
        changeType: ToolWindowManagerListener.ToolWindowManagerEventType
    ) {
        super.stateChanged(toolWindowManager, changeType)
        if (changeType == ToolWindowManagerListener.ToolWindowManagerEventType.ToolWindowAvailable) {
            runInEdt {
                toolWindowManager.getToolWindow("DevChat")?.let {
                    if (it.isAvailable) it.show()
                }
            }
        }
    }
}
