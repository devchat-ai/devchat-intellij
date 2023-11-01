package ai.devchat.idea;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefBrowserBase;
import com.intellij.ui.jcef.JBCefJSQuery;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.network.CefRequest;

import ai.devchat.common.Log;

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

        ActionHandler handler = ActionHandler.getInstance();
        handler.initialize(metadata, payload);

        Log.info("Got action: " + action);
        switch (action) {
            case Actions.SEND_MESSAGE_REQUEST:
                handler.executeAction(Actions.SEND_MESSAGE_REQUEST);
                break;
            case Actions.SET_OR_UPDATE_KEY_REQUEST:
                handler.executeAction(Actions.SET_OR_UPDATE_KEY_REQUEST);
                break;
            case Actions.LIST_COMMANDS_REQUEST:
                handler.executeAction(Actions.LIST_COMMANDS_REQUEST);
                break;
        }
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
