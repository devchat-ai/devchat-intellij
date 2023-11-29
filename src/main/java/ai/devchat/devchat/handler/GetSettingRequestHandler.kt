package ai.devchat.devchat.handler

import ai.devchat.common.Log
import ai.devchat.devchat.ActionHandler
import ai.devchat.devchat.DevChatActionHandler
import ai.devchat.devchat.DevChatActions
import ai.devchat.idea.setting.DevChatSettingsState
import ai.devchat.idea.storage.SensitiveDataStorage
import com.alibaba.fastjson.JSONObject

class GetSettingRequestHandler(private val devChatActionHandler: DevChatActionHandler) : ActionHandler {
    private var metadata: JSONObject? = null
    private var payload: JSONObject? = null
    override fun executeAction() {
        Log.info("Handling getSetting request")
        val callbackFunc = metadata!!.getString("callback")
        val settings = DevChatSettingsState.getInstance()
        val apiKey = SensitiveDataStorage.getKey()
        if (settings.apiBase == null || settings.apiBase.isEmpty()) {
            if (apiKey.startsWith("sk-")) {
                settings.apiBase = "https://api.openai.com/v1"
            } else if (apiKey.startsWith("DC.")) {
                settings.apiBase = "https://api.devchat.ai/v1"
            }
        }
        val apiBase = settings.apiBase
        val currentModel = settings.defaultModel
        devChatActionHandler.sendResponse(
            DevChatActions.GET_SETTING_RESPONSE,
            callbackFunc
        ) { metadata: JSONObject, payload: JSONObject ->
            metadata["status"] = "success"
            metadata["error"] = ""
            val setting = JSONObject()
            setting["apiKey"] = apiKey
            setting["apiBase"] = apiBase
            setting["currentModel"] = currentModel
            payload["setting"] = setting
        }
    }

    override fun setMetadata(metadata: JSONObject) {
        this.metadata = metadata
    }

    override fun setPayload(payload: JSONObject) {
        this.payload = payload
    }
}
