package ai.devchat.devchat.handler;

import ai.devchat.cli.DevChatWrapper;
import ai.devchat.common.DevChatPathUtil;
import ai.devchat.common.Log;
import ai.devchat.devchat.ActionHandler;
import ai.devchat.devchat.DevChatActionHandler;
import ai.devchat.devchat.DevChatActions;
import com.alibaba.fastjson.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeleteLastConversationRequestHandler implements ActionHandler {
    private JSONObject metadata;
    private JSONObject payload;
    private final DevChatActionHandler devChatActionHandler;

    public DeleteLastConversationRequestHandler(DevChatActionHandler devChatActionHandler) {
        this.devChatActionHandler = devChatActionHandler;
    }

    @Override
    public void executeAction() {
        Log.info("Handling delete last conversation request");
        String callbackFunc = metadata.getString("callback");
        String promptHash = payload.getString("promptHash");

        Map<String, List<String>> flags = new HashMap<>();
        flags.put("delete", Collections.singletonList(promptHash));

        String devchatCommandPath = DevChatPathUtil.getDevchatBinPath();
        DevChatWrapper devchatWrapper = new DevChatWrapper(devchatCommandPath);

        try {
            devchatWrapper.runLogCommand(flags);

            devChatActionHandler.sendResponse(DevChatActions.DELETE_LAST_CONVERSATION_RESPONSE, callbackFunc, (metadata, payload) -> {
                metadata.put("status", "success");
                metadata.put("error", "");
                payload.put("promptHash", promptHash);
            });
        } catch (Exception e) {
            devChatActionHandler.sendResponse(DevChatActions.DELETE_LAST_CONVERSATION_RESPONSE, callbackFunc, (metadata, payload) -> {
                metadata.put("status", "error");
                metadata.put("error", e.getMessage());
                payload.put("promptHash", promptHash);
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
