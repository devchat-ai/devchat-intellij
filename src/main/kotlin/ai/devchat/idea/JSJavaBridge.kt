package ai.devchat.idea

import ai.devchat.common.Log.info
import ai.devchat.devchat.ActionHandlerFactory
import com.alibaba.fastjson.JSON
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.network.CefRequest

class JSJavaBridge(private val jbCefBrowser: JBCefBrowser) {
    private val jsQuery: JBCefJSQuery

    init {
        jsQuery = JBCefJSQuery.create((jbCefBrowser as JBCefBrowserBase))
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
        val actionHandler = ActionHandlerFactory().createActionHandler(action)
        actionHandler.setMetadata(metadata)
        actionHandler.setPayload(payload)
        actionHandler.executeAction()
        return JBCefJSQuery.Response("ignore me")
    }

    fun registerToBrowser() {
        jbCefBrowser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadStart(browser: CefBrowser, frame: CefFrame, transitionType: CefRequest.TransitionType) {
                browser.executeJavaScript(
                    "window.JSJavaBridge = {"
                            + "callJava : function(arg) {"
                            + jsQuery.inject(
                        "arg",
                        "response => console.log(response)",
                        "(error_code, error_message) => console.log('callJava Failed', error_code, error_message)"
                    )
                            + "}"
                            + "};",
                    "", 0
                )
            }
        }, jbCefBrowser.cefBrowser)
    }
}
