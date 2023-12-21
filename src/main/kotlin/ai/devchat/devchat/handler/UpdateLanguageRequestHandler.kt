package ai.devchat.devchat.handler

import ai.devchat.devchat.BaseActionHandler
import ai.devchat.devchat.DevChatActions
import ai.devchat.idea.settings.DevChatSettingsState
import com.alibaba.fastjson.JSONObject

class UpdateLanguageRequestHandler(metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(metadata, payload) {
    override val actionName: String = DevChatActions.UPDATE_LANGUAGE_RESPONSE

    override fun action() {
        payload!!.getString("language")?.let {
            it.takeIf { it.isNotEmpty() }?.let {
                DevChatSettingsState.instance.language = if (it == "zh") "zh" else "en"
            }
        }
        send()
    }
}
