package ai.devchat.idea;

import ai.devchat.common.Log;
import ai.devchat.devchat.ActionHandler;
import ai.devchat.devchat.ActionHandlerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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

    private JBCefJSQuery.Response callJava(String arg) {
        Log.info("JSON string from JS: " + arg);

        String jsonArg = arg;
        if (arg.startsWith("\"") && arg.endsWith("\"")) {
            jsonArg = arg.substring(1, arg.length() - 1);
        }

        // Parse the json parameter
        JSONObject jsonObject = JSON.parseObject(jsonArg);
        String action = jsonObject.getString("action");
        JSONObject metadata = jsonObject.getJSONObject("metadata");
        JSONObject payload = jsonObject.getJSONObject("payload");
        Log.info("Got action: " + action);

        ActionHandler actionHandler = new ActionHandlerFactory().createActionHandler(action);
        actionHandler.setMetadata(metadata);
        actionHandler.setPayload(payload);
        actionHandler.executeAction();

        return new JBCefJSQuery.Response("ignore me");
    }

    public void registerToBrowser() {
        this.jbCefBrowser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadStart(CefBrowser browser, CefFrame frame, CefRequest.TransitionType transitionType) {
                browser.executeJavaScript(
                        "window.JSJavaBridge = {"
                                + "callJava : function(arg) {"
                                + jsQuery.inject("arg",
                                        "response => console.log(response)",
                                        "(error_code, error_message) => console.log('callJava Failed', error_code, error_message)")
                                + "}"
                                + "};",
                        "", 0);
            }
        }, this.jbCefBrowser.getCefBrowser());
    }
}
