package ai.devchat.idea;

import com.alibaba.fastjson.JSONObject;
import org.cef.browser.CefBrowser;

import java.util.HashMap;
import java.util.Map;

public class ActionHandler {

    private CefBrowser cefBrowser;
    private JSONObject metadata;
    private JSONObject payload;

    private Map<String, Runnable> actionMap;

    public ActionHandler(CefBrowser cefBrowser, JSONObject metadata, JSONObject payload) {
        this.cefBrowser = cefBrowser;
        this.metadata = metadata;
        this.payload = payload;
        actionMap = new HashMap<>();
        registerActions();
    }

    private void registerActions() {
        actionMap.put(Actions.SEND_MESSAGE_REQUEST, this::handleSendMessageRequest);
        actionMap.put(Actions.SET_OR_UPDATE_KEY_REQUEST, this::handleSetOrUpdateKeyRequest);
    }

    private void handleSendMessageRequest() {
        // do something with cefBrowser
        cefBrowser.executeJavaScript("alert('handleSendMessageRequest')", "", 0);
    }

    private void handleSetOrUpdateKeyRequest() {
        // do something with cefBrowser
        cefBrowser.executeJavaScript("alert('handleSetOrUpdateKeyRequest')", "", 0);
    }

    public void executeAction(String action) {
        Runnable actionHandler = actionMap.get(action);
        if (actionHandler != null) {
            actionHandler.run();
        }
    }

    public JSONObject getMetadata() {
        return metadata;
    }

    public void setMetadata(JSONObject metadata) {
        this.metadata = metadata;
    }

    public JSONObject getPayload() {
        return payload;
    }

    public void setPayload(JSONObject payload) {
        this.payload = payload;
    }
}
