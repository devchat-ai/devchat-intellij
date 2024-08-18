package ai.devchat.plugin

import ai.devchat.common.Log
import ai.devchat.core.DC_CLIENT
import ai.devchat.core.DevChatWrapper
import ai.devchat.installer.DevChatSetupThread
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefApp
import kotlinx.coroutines.*
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

class DevChatToolWindow : ToolWindowFactory, DumbAware, Disposable {
    private var ideService: IDEServer? = null
    private var localService: LocalService? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)


    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        currentProject = project
        val panel = JPanel(BorderLayout())
        if (!JBCefApp.isSupported()) {
            Log.error("JCEF is not supported.")
            panel.add(JLabel("JCEF is not supported", SwingConstants.CENTER))
        } else {
            panel.add(browser.jbCefBrowser.component, BorderLayout.CENTER)
        }
        panel.border = BorderFactory.createMatteBorder(0, 1, 0, 1, JBColor.LIGHT_GRAY)

        val content = toolWindow.contentManager.factory.createContent(panel, "", false)
        Disposer.register(content, this)
        toolWindow.contentManager.addContent(content)
        DevChatSetupThread().start()
        ideService = IDEServer(project).start()
        coroutineScope.launch {
            while (!pythonReady) { delay(100) }
            localService = LocalService().start()
        }
        toolWindow.setIcon(IconLoader.getIcon(DC_CLIENT.getIconUrl(), this::class.java.classLoader))
    }

    override fun dispose() {
        DevChatWrapper.activeChannel?.close()
        coroutineScope.cancel()
        ideService?.stop()
        localService?.stop()
    }

    companion object {
        var loaded: Boolean = false
        var pythonReady: Boolean = false
    }
}

var currentProject: Project? = null
