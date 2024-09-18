package ai.devchat.plugin

import ai.devchat.common.Log
import ai.devchat.core.DevChatWrapper
import ai.devchat.installer.DevChatSetupThread
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
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

class DevChatToolWindowFactory : ToolWindowFactory, DumbAware, Disposable {
    private var ideService: IDEServer? = null
    private var localService: LocalService? = null
    private var coroutineScope: CoroutineScope? = null
    var browser: Browser? = null

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        browser = Browser(project)
        val panel = JPanel(BorderLayout())
        if (!JBCefApp.isSupported()) {
            Log.error("JCEF is not supported.")
            panel.add(JLabel("JCEF is not supported", SwingConstants.CENTER))
        } else {
            panel.add(browser!!.jbCefBrowser.component, BorderLayout.CENTER)
        }
        panel.border = BorderFactory.createMatteBorder(0, 1, 0, 1, JBColor.LIGHT_GRAY)

        val content = toolWindow.contentManager.factory.createContent(panel, "", false)
        Disposer.register(content, this)
        toolWindow.contentManager.addContent(content)
        DevChatSetupThread(project).start()
        ideService = IDEServer(project).start()
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
            Log.error("Failed to start local service: ${exception.message}")
        }
        coroutineScope = CoroutineScope(Dispatchers.Default)
        coroutineScope!!.launch(coroutineExceptionHandler) {
            try {
                while (!pythonReady) {
                    delay(100)
                    ensureActive()
                }
                localService = LocalService().start()
                awaitCancellation()
            } finally {
                localService?.stop()
            }
        }
        project.getService(DevChatBrowserService::class.java).browser = browser
    }

    override fun dispose() {
        DevChatWrapper.activeChannel?.close()
        coroutineScope?.cancel()
        ideService?.stop()
        browser?.jbCefBrowser?.dispose()
    }

    companion object {
        var loaded: Boolean = false
        var pythonReady: Boolean = false
    }
}

var currentProject: Project? = null
