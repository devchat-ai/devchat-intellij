package ai.devchat.devchat.handler

import ai.devchat.common.Settings
import ai.devchat.devchat.BaseActionHandler
import ai.devchat.devchat.DevChatActions
import com.alibaba.fastjson.JSONObject



class GetSettingRequestHandler(metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(metadata, payload) {
    override val actionName: String = DevChatActions.GET_SETTING_RESPONSE
    override fun action() {
        val (apiKey, apiBase, defaultModel) = Settings.getAPISettings()
        send(payload= mapOf("setting" to mapOf(
            "apiKey" to apiKey,
            "apiBase" to apiBase,
            "currentModel" to defaultModel,
        )))
    }
}
