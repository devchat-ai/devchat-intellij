package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.storage.CONFIG
import com.alibaba.fastjson.JSONObject



class GetSettingRequestHandler(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.GET_SETTING_RESPONSE
    override fun action() {
        send(payload= mapOf("setting" to mapOf(
            "apiKey" to CONFIG["providers.devchat.api_key"],
            "apiBase" to CONFIG["providers.devchat.api_base"],
            "currentModel" to CONFIG["default_model"],
            "language" to CONFIG["language"]
        )))
    }
}
