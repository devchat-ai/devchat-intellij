package ai.devchat.idea;

import ai.devchat.cli.DevChatResponse;
import ai.devchat.cli.DevChatResponseConsumer;
import ai.devchat.cli.DevChatWrapper;
import ai.devchat.common.DevChatPathUtil;

import com.alibaba.fastjson.JSONObject;
import org.cef.browser.CefBrowser;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;


public class ActionHandler {

    private CefBrowser cefBrowser;
    private JSONObject metadata;
    private JSONObject payload;

    private Map<String, Runnable> actionMap;

    private int currentChunkId = 0;

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
        String message = payload.getString("message");

        Consumer<DevChatResponse> responseCallback = response -> {
            // Create the JSON response data
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("action", "sendMessage/response");

            // add metadata to jsonResponse
            JSONObject jsonMetadata = new JSONObject();
            currentChunkId += 1;
            jsonMetadata.put("currentChunkId", currentChunkId);
            jsonMetadata.put("isFinalChunk", false); // Update accordingly
            jsonMetadata.put("finishReason", ""); // Update accordingly
            jsonMetadata.put("error", ""); // Update accordingly
            jsonResponse.put("metadata", jsonMetadata);

            // add payload to jsonResponse
            JSONObject jsonPayload = new JSONObject();
            jsonPayload.put("message", response.getMessage());
            jsonPayload.put("user", response.getUser());
            jsonPayload.put("date", response.getDate());
            jsonPayload.put("promptHash", response.getPromptHash()); // Update accordingly
            jsonResponse.put("payload", jsonPayload);

            if (response.getPromptHash() != null) {
                jsonMetadata.put("isFinalChunk", true);
                currentChunkId = 0;
                jsonMetadata.put("finishReason", "success");
            }

            cefBrowser.executeJavaScript("alert('" + jsonResponse.toString() + "')", "", 0);
        };
        DevChatResponseConsumer responseConsumer = new DevChatResponseConsumer(responseCallback);

        Map<String, String> flags = new HashMap<>();
//        flags.put("flag_key", "flag_value");

        String devchatCommandPath = DevChatPathUtil.getDevchatBinPath();
        String apiKey = "your_api_key_here";

        DevChatWrapper devchatWrapper = new DevChatWrapper(apiKey, devchatCommandPath);
        devchatWrapper.runPromptCommand(flags, message, responseConsumer);
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
