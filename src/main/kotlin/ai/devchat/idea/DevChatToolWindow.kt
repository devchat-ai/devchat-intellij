package ai.devchat.idea

import ai.devchat.common.Log
import ai.devchat.common.ProjectUtils
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectCloseListener
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import java.awt.BorderLayout
import java.awt.Color
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

class DevChatToolWindow : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentManager = toolWindow.contentManager
        val content = contentManager.factory.createContent(
            DevChatToolWindowContent(project).content,
            "",
            false
        )
        contentManager.addContent(content)
        DevChatSetupThread().start()
        LanguageServer(project).start()
    }
}

internal class DevChatToolWindowContent(project: Project) {
    val content: JPanel
    private val project: Project

    init {
        Log.setLevelInfo()
        this.project = project
        content = JPanel(BorderLayout())
        // Check if JCEF is supported
        if (!JBCefApp.isSupported()) {
            Log.error("JCEF is not supported.")
            content.add(JLabel("JCEF is not supported", SwingConstants.CENTER))
            // TODO: 'return' is not allowed here
            // return
        }
        val jbCefBrowser = JBCefBrowser()
        content.add(jbCefBrowser.component, BorderLayout.CENTER)

        // Read static files
        var htmlContent = readStaticFile("/static/main.html")
        if (htmlContent!!.isEmpty()) {
            Log.error("main.html is missing.")
            htmlContent = "<html><body><h1>Error: main.html is missing.</h1></body></html>"
        }
        var jsContent = readStaticFile("/static/main.js")
        if (jsContent!!.isEmpty()) {
            Log.error("main.js is missing.")
            jsContent = "console.log('Error: main.js not found')"
        }
        val HtmlWithCssContent = insertCSSToHTML(htmlContent)
        val HtmlWithJsContent = insertJStoHTML(HtmlWithCssContent, jsContent)
        Log.info("main.html and main.js are loaded.")

        // enable dev tools
        val myDevTools = jbCefBrowser.cefBrowser.devTools
        JBCefBrowser.createBuilder()
            .setCefBrowser(myDevTools)
            .setClient(jbCefBrowser.jbCefClient)
            .build()

        // initialize DevChatActionHandler
        ProjectUtils.cefBrowser = jbCefBrowser.cefBrowser
        ProjectUtils.project = project

        // initialize JSJavaBridge
        val jsJavaBridge = JSJavaBridge(jbCefBrowser)
        jsJavaBridge.registerToBrowser()
        jbCefBrowser.loadHTML(HtmlWithJsContent!!)
    }

    private fun readStaticFile(fileName: String): String? {
        val contentBuilder = StringBuilder()
        try {
            val url = javaClass.getResource(fileName)
            if (url == null) {
                println("File not found: $fileName")
                return null
            }
            val reader = BufferedReader(InputStreamReader(url.openStream()))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                contentBuilder.append(line)
            }
            reader.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return contentBuilder.toString()
    }

    private fun insertJStoHTML(html: String?, js: String?): String? {
        var html = html
        val index = html!!.lastIndexOf("<script>")
        val endIndex = html.lastIndexOf("</script>")
        if (index != -1 && endIndex != -1) {
            html = """
                ${html.substring(0, index + "<script>".length)}
                $js${html.substring(endIndex)}
                """.trimIndent()
        }
        return html
    }

    private fun insertCSSToHTML(html: String?): String? {
        var html = html
        val index = html!!.lastIndexOf("<head>")
        val endIndex = html.lastIndexOf("</head>")
        val scheme = EditorColorsManager.getInstance().globalScheme
        val editorBgColor = scheme.getColor(EditorColors.CARET_ROW_COLOR)
        val foregroundColor = scheme.defaultForeground
        val styleTag = "<style>" + ":root{" +
                "--vscode-sideBar-background:" + colorToCssRgb(editorBgColor) + ";" +
                "--vscode-menu-background:" + colorToCssRgb(editorBgColor) + ";" +
                "--vscode-editor-foreground:" + colorToCssRgb(foregroundColor) + ";" +
                "--vscode-menu-foreground:" + colorToCssRgb(foregroundColor) + ";" +
                "--vscode-foreground:" + colorToCssRgb(foregroundColor) + ";" +
                "}" + "</style>"
        if (index != -1 && endIndex != -1) {
            html = """
                ${html.substring(0, index + "<head>".length)}
                $styleTag${html.substring(endIndex)}
                """.trimIndent()
        }
        return html
    }

    fun colorToCssRgb(color: Color?): String {
        return if (color != null) "rgb(" + color.red + "," + color.green + "," + color.blue + ")" else ""
    }
}
