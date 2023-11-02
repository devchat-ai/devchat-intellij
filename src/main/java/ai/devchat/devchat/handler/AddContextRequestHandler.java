package ai.devchat.devchat.handler;

import ai.devchat.devchat.ActionHandler;
import ai.devchat.devchat.DevChatActionHandler;
import com.alibaba.fastjson.JSONObject;

public class AddContextRequestHandler implements ActionHandler {
    private JSONObject metadata;
    private JSONObject payload;

    private final DevChatActionHandler devChatActionHandler;

    public AddContextRequestHandler(DevChatActionHandler devChatActionHandler) {
        this.devChatActionHandler = devChatActionHandler;
    }

    @Override
    public void executeAction() {
        devChatActionHandler.sendResponse("addContext/request", "AddContextFromEditor", (metadata, payload) -> {
            payload.put("file", payload.getString("file"));
            payload.put("content", payload.getString("content"));
        });
    }

    public void setMetadata(JSONObject metadata) {
        this.metadata = metadata;
    }

    public void setPayload(JSONObject payload) {
        this.payload = payload;
    }
}
