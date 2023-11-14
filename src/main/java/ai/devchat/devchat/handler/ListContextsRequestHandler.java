package ai.devchat.devchat.handler;

import ai.devchat.common.Log;
import ai.devchat.devchat.ActionHandler;
import ai.devchat.devchat.DevChatActionHandler;
import ai.devchat.devchat.DevChatActions;

import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListContextsRequestHandler implements ActionHandler {
    private JSONObject metadata;
    private JSONObject payload;
    private final DevChatActionHandler devChatActionHandler;

    public ListContextsRequestHandler(DevChatActionHandler devChatActionHandler) {
        this.devChatActionHandler = devChatActionHandler;
    }

    @Override
    public void executeAction() {
        Log.info("Handling list context request");

        String callbackFunc = metadata.getString("callback");

        List<Map<String, String>> contextList = new ArrayList<>();
        Map<String, String> context1 = new HashMap<>();
        context1.put("command", "git diff -cached");
        context1.put("description", "the staged changes since the last commit");

        Map<String, String> context2 = new HashMap<>();
        context2.put("command", "git diff HEAD");
        context2.put("description", "all changes since the last commit");

        contextList.add(context1);
        contextList.add(context2);

        devChatActionHandler.sendResponse(DevChatActions.LIST_CONTEXTS_RESPONSE, callbackFunc, (metadata, payload) -> {
            metadata.put("status", "success");
            metadata.put("error", "");

            payload.put("contexts", contextList);
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
