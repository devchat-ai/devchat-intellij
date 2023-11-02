package ai.devchat.devchat.handler;

import ai.devchat.common.Log;
import ai.devchat.devchat.ActionHandler;
import ai.devchat.devchat.DevChatActionHandler;
import ai.devchat.devchat.DevChatActions;
import ai.devchat.idea.storage.SensitiveDataStorage;
import com.alibaba.fastjson.JSONObject;

public class SetOrUpdateKeyRequestHandler implements ActionHandler {
    private JSONObject metadata;
    private JSONObject payload;
    private final DevChatActionHandler devChatActionHandler;

    public SetOrUpdateKeyRequestHandler(DevChatActionHandler devChatActionHandler) {
        this.devChatActionHandler = devChatActionHandler;
    }

    @Override
    public void executeAction() {
        Log.info("Handling set or update key request");

        String key = payload.getString("key");
        String callbackFunc = metadata.getString("callback");
        if (key == null || key.isEmpty()) {
            Log.error("Key is empty");
            devChatActionHandler.sendResponse(DevChatActions.SET_OR_UPDATE_KEY_RESPONSE, callbackFunc, (metadata, payload) -> {
                metadata.put("status", "error");
                metadata.put("error", "key is empty");
            });
        } else {
            SensitiveDataStorage.setKey(key);
            devChatActionHandler.sendResponse(DevChatActions.SET_OR_UPDATE_KEY_RESPONSE, callbackFunc, (metadata, payload) -> {
                metadata.put("status", "success");
                metadata.put("error", "");
            });
        }
    }

    public void setMetadata(JSONObject metadata) {
        this.metadata = metadata;
    }

    public void setPayload(JSONObject payload) {
        this.payload = payload;
    }
}
