package ai.devchat.devchat.handler

import ai.devchat.devchat.BaseActionHandler
import ai.devchat.devchat.DevChatActions
import ai.devchat.idea.settings.DevChatSettingsState
import com.alibaba.fastjson.JSONObject

class UpdateSettingRequestHandler(metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(metadata, payload) {
    override val actionName: String = DevChatActions.UPDATE_SETTING_RESPONSE

    override fun action() {
        payload!!.getJSONObject("setting")
            .getString("currentModel")?.let {
                DevChatSettingsState.instance.defaultModel = it
            }
        send()
    }
}
