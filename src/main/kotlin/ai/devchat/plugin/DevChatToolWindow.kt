package ai.devchat.plugin

import ai.devchat.core.DevChatWrapper
import ai.devchat.common.Log
import ai.devchat.common.ProjectUtils
import ai.devchat.installer.DevChatSetupThread
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.*
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBuilder
import java.awt.BorderLayout
import java.awt.Color
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

class DevChatToolWindow : ToolWindowFactory, DumbAware, Disposable {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel(BorderLayout())
        if (!JBCefApp.isSupported()) {
            Log.error("JCEF is not supported.")
            panel.add(JLabel("JCEF is not supported", SwingConstants.CENTER))
        } else {
            val jbCefBrowser = JBCefBrowserBuilder()
                .setOffScreenRendering(false)
                .build()
            JBCefBrowserBuilder()
                .setCefBrowser(jbCefBrowser.cefBrowser.devTools)
                .setClient(jbCefBrowser.jbCefClient)
                .build()
            panel.add(jbCefBrowser.component, BorderLayout.CENTER)

            // initialize DevChatActionHandler
            ProjectUtils.apply {
                cefBrowser = jbCefBrowser.cefBrowser
                ProjectUtils.project = project
            }

            // initialize JSJavaBridge
            JSJavaBridge(jbCefBrowser).registerToBrowser()
            jbCefBrowser.loadHTML(UIBuilder().content)
        }

        val content = toolWindow.contentManager.factory.createContent(panel, "", false)
        Disposer.register(content, this)
        toolWindow.contentManager.addContent(content)
        DevChatSetupThread().start()
        IDEServer(project).start()
    }

    companion object {
        var loaded: Boolean = false
    }

    override fun dispose() {
        DevChatWrapper.activeChannel?.close()
    }
}

class UIBuilder(private val staticPath: String = "/static") {
    val content: String = buildHTML()

    private fun buildHTML(): String {
        // Read static files
        var html = loadResource(Paths.get(staticPath, "main.html").toString())
        if (html.isNullOrEmpty()) {
            Log.error("main.html is missing.")
            html = "<html><body><h1>Error: main.html is missing.</h1></body></html>"
        }
        var js = loadResource(Paths.get(staticPath,"main.js").toString())
        if (js.isNullOrEmpty()) {
            Log.error("main.js is missing.")
            js = "console.log('Error: main.js not found')"
        }
        html = insertCSS(html)
        html = insertJS(html, js)
        Log.info("main.html and main.js are loaded.")
        return html
    }

    private fun loadResource(fileName: String): String? = runCatching {
        javaClass.getResource(fileName)?.readText(StandardCharsets.UTF_8) ?: run {
            println("File not found: $fileName")
            null
        }
    }.onFailure {
        it.printStackTrace()
    }.getOrNull()

    private fun insertJS(html: String, js: String?): String {
        val scriptTag = "<script>"
        val index = html.lastIndexOf(scriptTag)
        val endIndex = html.lastIndexOf("</script>")
        return if (index != -1 && endIndex != -1) {
            html.substring(0, index + scriptTag.length) + js + html.substring(endIndex)
        } else {
            html
        }
    }

    private fun insertCSS(html: String): String {
        val scheme = EditorColorsManager.getInstance().globalScheme
        val rowColor = scheme.getColor(EditorColors.CARET_ROW_COLOR)?.toRGB()
        val editorBgColor = scheme.defaultBackground.toRGB()
        val foregroundColor = scheme.defaultForeground.toRGB()
        val fontSize = scheme.editorFontSize
        val styleTag = buildString {
            append("<style>:root{")
            append("--vscode-sideBar-background:$editorBgColor;")
            append("--vscode-menu-background:$editorBgColor;")
            append("--vscode-editor-foreground:$foregroundColor;")
            append("--vscode-menu-foreground:$foregroundColor;")
            append("--vscode-foreground:$foregroundColor;")
            append("--vscode-commandCenter-activeBackground:$rowColor;")
            append("--vscode-editor-font-size:${fontSize}px;")
            append("}</style>")
        }
        return html.replace("</head>", "$styleTag</head>")
    }

    private fun Color.toRGB(): String {
        return "rgb($red,$green,$blue)"
    }

}
