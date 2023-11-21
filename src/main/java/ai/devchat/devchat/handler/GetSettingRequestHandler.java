package ai.devchat.devchat.handler;

import ai.devchat.common.Log;
import ai.devchat.devchat.ActionHandler;
import ai.devchat.devchat.DevChatActionHandler;
import ai.devchat.devchat.DevChatActions;
import ai.devchat.idea.setting.DevChatSettingsState;
import ai.devchat.idea.storage.SensitiveDataStorage;
import com.alibaba.fastjson.JSONObject;

public class GetSettingRequestHandler implements ActionHandler {
    private JSONObject metadata;
    private JSONObject payload;
    private final DevChatActionHandler devChatActionHandler;

    public GetSettingRequestHandler(DevChatActionHandler devChatActionHandler) {
        this.devChatActionHandler = devChatActionHandler;
    }

    @Override
    public void executeAction() {
        Log.info("Handling getSetting request");
        String callbackFunc = metadata.getString("callback");

        DevChatSettingsState settings = DevChatSettingsState.getInstance();
        String apiKey = SensitiveDataStorage.getKey();
        if (settings.apiBase == null || settings.apiBase.isEmpty()) {
            if (apiKey.startsWith("sk-")) {
                settings.apiBase = "https://api.openai.com/v1";
            } else if (apiKey.startsWith("DC.")) {
                settings.apiBase = "https://api.devchat.ai/v1";
            }
        }

        String apiBase = settings.apiBase;
        String currentModel = settings.defaultModel;

        devChatActionHandler.sendResponse(DevChatActions.GET_SETTING_RESPONSE, callbackFunc, (metadata, payload) -> {
            metadata.put("status", "success");
            metadata.put("error", "");
            JSONObject setting = new JSONObject();
            setting.put("apiKey", apiKey);
            setting.put("apiBase", apiBase);
            setting.put("currentModel", currentModel);
            payload.put("setting", setting);
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
