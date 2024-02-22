package ai.devchat.plugin

import ai.devchat.core.DevChatWrapper
import ai.devchat.common.Log
import ai.devchat.installer.DevChatSetupThread
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.*
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.jcef.JBCefApp
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

class DevChatToolWindow : ToolWindowFactory, DumbAware, Disposable {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        currentProject = project
        val panel = JPanel(BorderLayout())
        if (!JBCefApp.isSupported()) {
            Log.error("JCEF is not supported.")
            panel.add(JLabel("JCEF is not supported", SwingConstants.CENTER))
        } else {
            panel.add(browser.jbCefBrowser.component, BorderLayout.CENTER)
        }

        val content = toolWindow.contentManager.factory.createContent(panel, "", false)
        Disposer.register(content, this)
        toolWindow.contentManager.addContent(content)
        DevChatSetupThread().start()
        IDEServer(project).start()
    }

    override fun dispose() {
        DevChatWrapper.activeChannel?.close()
    }

    companion object {
        var loaded: Boolean = false
        val browser: Browser = Browser()
    }
}

var currentProject: Project? = null
