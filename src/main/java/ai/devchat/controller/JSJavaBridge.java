package ai.devchat.controller;

import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefBrowserBase;
import com.intellij.ui.jcef.JBCefJSQuery;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.network.CefRequest;

public class JSJavaBridge {

    private final JBCefBrowser jbCefBrowser;
    private final JBCefJSQuery jsQuery;

    public JSJavaBridge(JBCefBrowser jbCefBrowser) {
        this.jbCefBrowser = jbCefBrowser;
        this.jsQuery = JBCefJSQuery.create((JBCefBrowserBase) this.jbCefBrowser);
        this.jsQuery.addHandler(this::callJava);
    }

    private JBCefJSQuery.Response callJava(String args) {
        System.out.println("args from js: " + args);
        if ("null".equals(args)) {
            return new JBCefJSQuery.Response(null, 1, "got null");
        } else if ("undefined".equals(args)) {
            return new JBCefJSQuery.Response(null);
        }
        return new JBCefJSQuery.Response("Response from java: you sent " + args);
    }

    public void registerToBrowser() {
        this.jbCefBrowser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadStart(CefBrowser browser, CefFrame frame, CefRequest.TransitionType transitionType) {
                browser.executeJavaScript(
                    "window.JSJavaBridge = {"
                        + "callJava : function(arg) {"
                        + jsQuery.inject("JSON.stringify(arg)",
                        "response => displayResponseFromJava(response)",
                        "(error_code, error_message) => console.log('callJava Failed', error_code, error_message)")
                        + "}"
                        + "};",
                    "", 0);
            }
        }, this.jbCefBrowser.getCefBrowser());
    }
}
