package ai.devchat.idea

import ai.devchat.common.Log.info
import ai.devchat.devchat.ActionHandlerFactory
import ai.devchat.devchat.handler.AddContextNotifyHandler
import ai.devchat.devchat.handler.SendUserMessageHandler
import ai.devchat.idea.action.AddToDevChatAction
import com.alibaba.fastjson.JSON
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.network.CefRequest

class JSJavaBridge(private val jbCefBrowser: JBCefBrowser) {
    val jsQuery = JBCefJSQuery.create(jbCefBrowser as JBCefBrowserBase)

    init {
        jsQuery.addHandler { arg: String -> callJava(arg) }
    }

    private fun callJava(arg: String): JBCefJSQuery.Response {
        info("JSON string from JS: $arg")
        var jsonArg = arg
        if (arg.startsWith("\"") && arg.endsWith("\"")) {
            jsonArg = arg.substring(1, arg.length - 1)
        }

        // Parse the json parameter
        val jsonObject = JSON.parseObject(jsonArg)
        val action = jsonObject.getString("action")
        val metadata = jsonObject.getJSONObject("metadata")
        val payload = jsonObject.getJSONObject("payload")
        info("Got action: $action")
        ActionHandlerFactory().createActionHandler(action, metadata, payload)?.let {
            ApplicationManager.getApplication().invokeLater {
                it.executeAction()
            }
        }
        return JBCefJSQuery.Response("ignore me")
    }

    fun registerToBrowser() {

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
                    AddContextNotifyHandler(null, AddToDevChatAction.cache).executeAction()
                    AddToDevChatAction.cache = null
                }
                if (UnitTestsCVProvider.cache != null) {
                    SendUserMessageHandler(null, UnitTestsCVProvider.cache).executeAction()
                    UnitTestsCVProvider.cache = null
                }
                DevChatToolWindow.loaded = true
            }
        }, jbCefBrowser.cefBrowser)
    }
}
