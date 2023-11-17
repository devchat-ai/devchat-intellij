package ai.devchat.devchat.handler;

import ai.devchat.devchat.ActionHandler;
import ai.devchat.devchat.DevChatActionHandler;
import ai.devchat.devchat.DevChatActions;

import com.alibaba.fastjson.JSONObject;

public class AddContextNotifyHandler implements ActionHandler {
    private JSONObject metadata;
    private JSONObject payload;
    private final DevChatActionHandler devChatActionHandler;

    public final String RESPONSE_FUNC = "IdeaToJSMessage";

    public AddContextNotifyHandler(DevChatActionHandler devChatActionHandler) {
        this.devChatActionHandler = devChatActionHandler;
    }

    @Override
    public void executeAction() {
        devChatActionHandler.sendResponse(DevChatActions.ADD_CONTEXT_NOTIFY, RESPONSE_FUNC, (metadata, payload) -> {
            metadata.put("status", "success");
            metadata.put("error", "");

            payload.put("path", this.payload.getString("path"));
            payload.put("content", this.payload.getString("content"));
            payload.put("languageId", this.payload.getString("languageId"));
            payload.put("startLine", this.payload.getInteger("startLine"));
        });
    }

    public void setMetadata(JSONObject metadata) {
        this.metadata = metadata;
    }

    public void setPayload(JSONObject payload) {
        this.payload = payload;
    }
}
