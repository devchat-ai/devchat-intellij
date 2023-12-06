package ai.devchat.devchat.handler

import ai.devchat.devchat.ActionHandler
import ai.devchat.devchat.DevChatActionHandler
import ai.devchat.devchat.DevChatActions
import ai.devchat.idea.settings.DevChatSettingsState
import com.alibaba.fastjson.JSONObject

class UpdateSettingRequestHandler(private val devChatActionHandler: DevChatActionHandler) : ActionHandler {
    private var metadata: JSONObject? = null
    private var payload: JSONObject? = null
    override fun executeAction() {
        val callbackFunc = metadata!!.getString("callback")
        val setting = payload!!.getJSONObject("setting")
        try {
            val settings = DevChatSettingsState.instance
            if (setting.containsKey("currentModel")) {
                settings.defaultModel = setting.getString("currentModel")
            }
            devChatActionHandler.sendResponse(
                DevChatActions.UPDATE_SETTING_RESPONSE,
                callbackFunc
            ) { metadata: JSONObject, payload: JSONObject? ->
                metadata["status"] = "success"
                metadata["error"] = ""
            }
        } catch (e: Exception) {
            devChatActionHandler.sendResponse(
                DevChatActions.UPDATE_SETTING_RESPONSE,
                callbackFunc
            ) { metadata: JSONObject, payload: JSONObject? ->
                metadata["status"] = "Failed"
                metadata["error"] = "Failed to update setting." + e.message
            }
        }
    }

    override fun setMetadata(metadata: JSONObject) {
        this.metadata = metadata
    }

    override fun setPayload(payload: JSONObject) {
        this.payload = payload
    }
}
