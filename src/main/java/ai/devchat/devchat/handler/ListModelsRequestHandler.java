package ai.devchat.devchat.handler;

import ai.devchat.common.Log;
import ai.devchat.devchat.ActionHandler;
import ai.devchat.devchat.DevChatActionHandler;
import ai.devchat.devchat.DevChatActions;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ListModelsRequestHandler implements ActionHandler {
    private JSONObject metadata;
    private JSONObject payload;
    private final DevChatActionHandler devChatActionHandler;

    public ListModelsRequestHandler(DevChatActionHandler devChatActionHandler) {
        this.devChatActionHandler = devChatActionHandler;
    }

    @Override
    public void executeAction() {
        Log.info("Handling list model request");

        String callbackFunc = metadata.getString("callback");

        List<String> modelList = new ArrayList<>();
        modelList.add("gpt-3.5-turbo");
        modelList.add("gpt-4");
        modelList.add("gpt-3.5-turbo-16k");
        modelList.add("claude-2");

        devChatActionHandler.sendResponse(DevChatActions.LIST_MODELS_RESPONSE, callbackFunc, (metadata, payload) -> {
            metadata.put("status", "success");
            metadata.put("error", "");

            payload.put("models", modelList);
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
