package ai.devchat.devchat.handler;

import ai.devchat.devchat.ActionHandler;
import ai.devchat.devchat.DevChatActionHandler;
import ai.devchat.devchat.DevChatActions;
import ai.devchat.idea.storage.DeletedTopicsState;
import com.alibaba.fastjson.JSONObject;

public class DeleteTopicRequestHandler implements ActionHandler {
    private JSONObject metadata;
    private JSONObject payload;
    private final DevChatActionHandler devChatActionHandler;

    public DeleteTopicRequestHandler(DevChatActionHandler devChatActionHandler) {
        this.devChatActionHandler = devChatActionHandler;
    }

    @Override
    public void executeAction() {
        String callbackFunc = metadata.getString("callback");
        String topicHash = payload.getString("topicHash");

        DeletedTopicsState state = DeletedTopicsState.getInstance();

        if (!state.deletedTopicHashes.contains(topicHash)) {
            state.deletedTopicHashes.add(topicHash);
        }

        devChatActionHandler.sendResponse(DevChatActions.DELETE_TOPIC_RESPONSE, callbackFunc, (metadata, payload) -> {
            metadata.put("status", "success");
            metadata.put("error", "");
            payload.put("topicHash", topicHash);
        });
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
