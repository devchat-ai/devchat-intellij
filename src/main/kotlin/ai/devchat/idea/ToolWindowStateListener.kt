package ai.devchat.idea

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener


class ToolWindowStateListener : ToolWindowManagerListener {
    override fun toolWindowsRegistered(ids: MutableList<String>, toolWindowManager: ToolWindowManager) {
        super.toolWindowsRegistered(ids, toolWindowManager)
        for (id in ids) {
            if (id == "DevChat") {
                runInEdt {
                    toolWindowManager.getToolWindow("DevChat")?.let {
                        while (!it.isAvailable) {
                            Thread.sleep(1000)
                        }
                        it.show()
                    }
                }
            }
        }
    }
}
