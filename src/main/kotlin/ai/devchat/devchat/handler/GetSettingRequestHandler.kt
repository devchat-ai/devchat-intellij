package ai.devchat.devchat.handler

import ai.devchat.common.Settings
import ai.devchat.devchat.BaseActionHandler
import ai.devchat.devchat.DevChatActions
import ai.devchat.idea.settings.DevChatSettingsState
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
