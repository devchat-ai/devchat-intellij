package ai.devchat.core.handlers

import ai.devchat.common.Settings
import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.storage.DevChatSettingsState
import com.alibaba.fastjson.JSONObject



class GetSettingRequestHandler(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.GET_SETTING_RESPONSE
    override fun action() {
        val (apiKey, apiBase, defaultModel) = Settings.getAPISettings()
        send(payload= mapOf("setting" to mapOf(
            "apiKey" to apiKey,
            "apiBase" to apiBase,
            "currentModel" to defaultModel,
            "language" to DevChatSettingsState.instance.language,
        )))
    }
}
