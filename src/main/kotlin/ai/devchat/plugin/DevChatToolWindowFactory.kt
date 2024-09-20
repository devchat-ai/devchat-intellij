package ai.devchat.plugin

import ai.devchat.common.Log
import ai.devchat.core.DevChatClient
import ai.devchat.core.DevChatWrapper
import ai.devchat.installer.DevChatSetupThread
import ai.devchat.storage.ActiveConversation
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
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

@Service(Service.Level.PROJECT)
class DevChatService(project: Project) {
    var activeConversation: ActiveConversation? = null
    var browser: Browser? = null
    var localServicePort: Int? = null
    var ideServicePort: Int? = null
    var client: DevChatClient? = null
    var wrapper: DevChatWrapper? = null
    var pythonReady: Boolean = false
    var uiLoaded: Boolean = false
}

class DevChatToolWindowFactory : ToolWindowFactory, DumbAware, Disposable {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val devChatService = project.getService(DevChatService::class.java)
        val browser = Browser(project)
        devChatService.browser = browser

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
        Disposer.register(content, browser)

        DevChatSetupThread(project).start()

        CoroutineScope(Dispatchers.IO).launch {
            withTimeoutOrNull(5000) {
                while (!devChatService.pythonReady) { delay(100) }
                LocalService(project).start()
            }?.let {
                Disposer.register(content, it)
                devChatService.localServicePort = it.port!!
                devChatService.client = DevChatClient(project, it.port!!)
            }
        }
        IDEServer(project).start().let {
            Disposer.register(content, it)
            devChatService.ideServicePort = it.port
        }
        DevChatWrapper(project).let {
            Disposer.register(content, it)
            devChatService.wrapper = it
        }
        devChatService.activeConversation = ActiveConversation()

        toolWindow.contentManager.addContent(content)
    }

//    private fun startLocalService() {
//        val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
//            Log.error("Failed to start local service: ${exception.message}")
//        }
//        coroutineScope = CoroutineScope(Dispatchers.Default)
//        coroutineScope!!.launch(coroutineExceptionHandler) {
//            try {
//                while (!pythonReady) {
//                    delay(100)
//                    ensureActive()
//                }
//                localService = LocalService().start()
//                awaitCancellation()
//            } finally {
//                localService?.stop()
//            }
//        }
//    }


    override fun dispose() {}
}
