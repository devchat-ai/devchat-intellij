package ai.devchat.idea;

import ai.devchat.cli.DevChatResponse;
import ai.devchat.cli.DevChatResponseConsumer;
import ai.devchat.cli.DevChatWrapper;
import ai.devchat.common.DevChatPathUtil;
import ai.devchat.common.Log;

import com.alibaba.fastjson.JSONObject;
import org.cef.browser.CefBrowser;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ActionHandler {

    private static ActionHandler instance;
    private CefBrowser cefBrowser;
    private JSONObject metadata;
    private JSONObject payload;

    private Map<String, Runnable> actionMap;

    private int currentChunkId = 0;

    private ActionHandler() {
        actionMap = new HashMap<>();
        registerActions();
    }

    public static synchronized ActionHandler getInstance() {
        if (instance == null) {
            instance = new ActionHandler();
        }
        return instance;
    }

    public void initialize(CefBrowser cefBrowser) {
        this.cefBrowser = cefBrowser;
    }

    public void initialize(JSONObject metadata, JSONObject payload) {
        this.metadata = metadata;
        this.payload = payload;
    }

    private void sendResponse(String action, BiConsumer<JSONObject, JSONObject> callback) {
        JSONObject response = new JSONObject();
        response.put("action", action);

        JSONObject metadata = new JSONObject();
        JSONObject payload = new JSONObject();

        response.put("metadata", metadata);
        response.put("payload", payload);

        callback.accept(metadata, payload);

        cefBrowser.executeJavaScript("alert('" + response.toString() + "')", "", 0);
    }

    private void registerActions() {
        actionMap.put(Actions.SEND_MESSAGE_REQUEST, this::handleSendMessageRequest);
        actionMap.put(Actions.SET_OR_UPDATE_KEY_REQUEST, this::handleSetOrUpdateKeyRequest);
    }

    private void handleSendMessageRequest() {
        String message = payload.getString("message");

        try {
            Map<String, String> flags = new HashMap<>();
            // flags.put("flag_key", "flag_value");

            String devchatCommandPath = DevChatPathUtil.getDevchatBinPath();
            String apiKey = "your_api_key_here";

            DevChatResponseConsumer responseConsumer = getResponseConsumer();
            DevChatWrapper devchatWrapper = new DevChatWrapper(apiKey, devchatCommandPath);
            devchatWrapper.runPromptCommand(flags, message, responseConsumer);
        } catch (Exception e) {
            Log.error("Exception occrred while executing DevChat command. Exception message: " + e.getMessage());

            sendResponse(Actions.SEND_MESSAGE_RESPONSE, (metadata, payload) -> {
                metadata.put("currentChunkId", 0);
                metadata.put("isFinalChunk", true);
                metadata.put("finishReason", "error");
                metadata.put("error", e.getMessage());
            });
        }
    }

    @NotNull
    private DevChatResponseConsumer getResponseConsumer() {
        Consumer<DevChatResponse> jsCallback = response -> {
            sendResponse(Actions.SEND_MESSAGE_RESPONSE, (metadata, payload) -> {
                currentChunkId += 1;
                metadata.put("currentChunkId", currentChunkId);
                metadata.put("isFinalChunk", response.getPromptHash() == null);
                metadata.put("finishReason", response.getPromptHash() != null ? "success" : "");
                metadata.put("error", "");

                payload.put("message", response.getMessage());
                payload.put("user", response.getUser());
                payload.put("date", response.getDate());
                payload.put("promptHash", response.getPromptHash());
            });
        };
        return new DevChatResponseConsumer(jsCallback);
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
