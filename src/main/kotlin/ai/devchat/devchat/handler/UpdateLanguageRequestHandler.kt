package ai.devchat.devchat.handler

import ai.devchat.devchat.BaseActionHandler
import ai.devchat.devchat.DevChatActions
import ai.devchat.idea.settings.DevChatSettingsState
import com.alibaba.fastjson.JSONObject

class UpdateLanguageRequestHandler(metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(metadata, payload) {
    override val actionName: String = DevChatActions.UPDATE_LANGUAGE_RESPONSE

    override fun action() {
        DevChatSettingsState.instance.language = payload!!.getString("language")
        send()
    }
}
