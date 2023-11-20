package ai.devchat.devchat.handler;

import ai.devchat.devchat.ActionHandler;
import ai.devchat.devchat.DevChatActionHandler;
import ai.devchat.idea.storage.SensitiveDataStorage;
import com.alibaba.fastjson.JSONObject;

public class GetKeyRequestHandler implements ActionHandler {
    private JSONObject metadata;
    private JSONObject payload;
    private final DevChatActionHandler devChatActionHandler;

    public GetKeyRequestHandler(DevChatActionHandler devChatActionHandler) {
        this.devChatActionHandler = devChatActionHandler;
    }

    @Override
    public void executeAction() {
        String callbackFunc = metadata.getString("callback");
        String key = SensitiveDataStorage.getKey();

        if (key != null && !key.isEmpty()) {
            devChatActionHandler.sendResponse("getKey/response", callbackFunc, (metadata, payload) -> {
                metadata.put("status", "success");
                metadata.put("error", "");
                payload.put("key", key);
            });
        } else {
            devChatActionHandler.sendResponse("getKey/response", callbackFunc, (metadata, payload) -> {
                metadata.put("status", "error");
                metadata.put("error", "key is empty");
            });
        }
    }

    @Override
    public void setMetadata(JSONObject metadata) {
        this.metadata = metadata;
    }

    @Override
    public void setPayload(JSONObject payload) {
        this.payload = payload;
    }
}
