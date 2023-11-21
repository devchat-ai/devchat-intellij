package ai.devchat.devchat.handler;

import ai.devchat.devchat.ActionHandler;
import ai.devchat.devchat.DevChatActionHandler;
import ai.devchat.devchat.DevChatActions;
import ai.devchat.idea.setting.DevChatSettingsState;
import com.alibaba.fastjson.JSONObject;

public class UpdateSettingRequestHandler implements ActionHandler {
    private JSONObject metadata;
    private JSONObject payload;
    private final DevChatActionHandler devChatActionHandler;

    public UpdateSettingRequestHandler(DevChatActionHandler devChatActionHandler) {
        this.devChatActionHandler = devChatActionHandler;
    }

    @Override
    public void executeAction() {
        String callbackFunc = metadata.getString("callback");
        JSONObject setting = payload.getJSONObject("setting");

        try {
            DevChatSettingsState settings = DevChatSettingsState.getInstance();
            if (setting.containsKey("currentModel")) {
                settings.defaultModel = setting.getString("currentModel");
            }

            devChatActionHandler.sendResponse(DevChatActions.UPDATE_SETTING_RESPONSE, callbackFunc, (metadata, payload) -> {
                metadata.put("status", "success");
                metadata.put("error", "");
            });
        } catch (Exception e) {
            devChatActionHandler.sendResponse(DevChatActions.UPDATE_SETTING_RESPONSE, callbackFunc, (metadata, payload) -> {
                metadata.put("status", "Failed");
                metadata.put("error", "Failed to update setting." + e.getMessage());
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
