package ai.devchat.plugin

import ai.devchat.common.Log
import ai.devchat.core.ActionHandlerFactory
import ai.devchat.core.DevChatActions
import ai.devchat.core.handlers.AddContextNotifyHandler
import ai.devchat.core.handlers.SendUserMessageHandler
import ai.devchat.plugin.actions.AddToDevChatAction
import com.alibaba.fastjson.JSON
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefBrowserBuilder
import com.intellij.ui.jcef.JBCefJSQuery
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.network.CefRequest
import java.awt.Color
import java.nio.charset.StandardCharsets

@Service(Service.Level.PROJECT)
class DevChatBrowserService(project: Project) {
    var browser: Browser? = null
}

class Browser(val project: Project) {
    val jbCefBrowser = JBCefBrowserBuilder().setOffScreenRendering(false).setEnableOpenDevToolsMenuItem(true).build()

    init {
        registerLoadHandler()
        loadUI()
    }

    fun executeJS(func: String, arg: Any? = null) {
        val funcCall = if (arg == null) "$func()" else "$func($arg)"
        jbCefBrowser.cefBrowser.executeJavaScript(funcCall, "", 0)
    }

    private fun callJava(arg: String): JBCefJSQuery.Response {
        Log.info("JSON string from JS: $arg")
        var jsonArg = arg
        if (arg.startsWith("\"") && arg.endsWith("\"")) {
            jsonArg = arg.substring(1, arg.length - 1)
        }

        // Parse the json parameter
        val jsonObject = JSON.parseObject(jsonArg)
        val action = jsonObject.getString("action")
        val metadata = jsonObject.getJSONObject("metadata")
        val payload = jsonObject.getJSONObject("payload")
        Log.info("Got action: $action")
        ActionHandlerFactory().createActionHandler(project, action, metadata, payload)?.let {
            ApplicationManager.getApplication().invokeLater {
                it.executeAction()
            }
        }
        return JBCefJSQuery.Response("ignore me")
    }

    private fun loadUI() {
        jbCefBrowser.loadHTML(UIBuilder().content)
    }

    private fun registerLoadHandler() {
        val jsQuery = JBCefJSQuery.create(jbCefBrowser as JBCefBrowserBase)
        jsQuery.addHandler { arg: String -> callJava(arg) }

        jbCefBrowser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadStart(browser: CefBrowser?, frame: CefFrame?, transitionType: CefRequest.TransitionType?) {
                jbCefBrowser.cefBrowser.executeJavaScript("""
                    window.JSJavaBridge = {callJava : function(arg) {
                        ${jsQuery.inject(
                            "arg",
                "response => console.log(response)",
                "(error_code, error_message) => console.log('callJava Failed', error_code, error_message)"
                        )}
                        }};
                    """.trimIndent(),
                    jbCefBrowser.cefBrowser.url, 0
                )
            }

            override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                if (AddToDevChatAction.cache != null) {
                    AddContextNotifyHandler(
                        project, DevChatActions.ADD_CONTEXT_NOTIFY,null, AddToDevChatAction.cache
                    ).executeAction()
                    AddToDevChatAction.cache = null
                }
                if (SendUserMessageHandler.cache != null) {
                    SendUserMessageHandler(
                        project, DevChatActions.SEND_USER_MESSAGE_REQUEST,null, SendUserMessageHandler.cache
                    ).executeAction()
                    SendUserMessageHandler.cache = null
                }
                DevChatToolWindowFactory.loaded = true
            }
        }, jbCefBrowser.cefBrowser)
    }
}

class UIBuilder(private val staticResource: String = "/static") {
    val content: String = buildHTML()

    private fun buildHTML(): String {
        // Read static files
        var html = loadResource("$staticResource/main.html")
        if (html.isNullOrEmpty()) {
            Log.error("main.html is missing.")
            html = "<html><body><h1>Error: main.html is missing.</h1></body></html>"
        }
        var js = loadResource("$staticResource/main.js")
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
