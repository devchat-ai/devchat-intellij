package ai.devchat.idea;

import ai.devchat.cli.DevChatResponse;
import ai.devchat.cli.DevChatResponseConsumer;
import ai.devchat.cli.DevChatWrapper;
import ai.devchat.common.DevChatPathUtil;
import ai.devchat.common.Log;
import ai.devchat.idea.setting.DevChatSettingsState;

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
        actionMap.put(Actions.ADD_CONTEXT_REQUEST, this::handleAddContextRequest);
    }

    private void handleSendMessageRequest() {
        String message = payload.getString("message");
        String context = payload.getString("context");
        String parent = metadata.getString("parent");

        try {
            Map<String, String> flags = new HashMap<>();
            if (context != null && !context.isEmpty()) {
                flags.put("context", context);
            }
            if (parent != null && !parent.isEmpty()) {
                flags.put("parent", parent);
            }

            String devchatCommandPath = DevChatPathUtil.getDevchatBinPath();
            String apiKey = SensitiveDataStorage.getKey();

            String apiBase = "https://change.me";
            DevChatSettingsState settings = DevChatSettingsState.getInstance();
            if (settings.apiBase != null && !settings.apiBase.isEmpty()) {
                apiBase = settings.apiBase;
            }

            DevChatResponseConsumer responseConsumer = getResponseConsumer();
            DevChatWrapper devchatWrapper = new DevChatWrapper(apiBase, apiKey, devchatCommandPath);
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

    private void handleSetOrUpdateKeyRequest() {
        String key = payload.getString("key");
        if (key == null || key.isEmpty()) {
            Log.error("Key is empty");
            sendResponse(Actions.SET_OR_UPDATE_KEY_RESPONSE, (metadata, payload) -> {
                metadata.put("status", "error");
                metadata.put("error", "key is empty");
            });
        } else {
            SensitiveDataStorage.setKey(key);
            sendResponse(Actions.SET_OR_UPDATE_KEY_RESPONSE, (metadata, payload) -> {
                metadata.put("status", "success");
                metadata.put("error", "");
            });
        }
    }

    private void handleAddContextRequest() {
        sendResponse("addContext/request", (metadata, payload) -> {
            payload.put("file", this.payload.getString("file"));
            payload.put("content", this.payload.getString("content"));
        });
    }

    public void executeAction(String action) {
        Runnable actionHandler = actionMap.get(action);
        if (actionHandler != null) {
            actionHandler.run();
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
