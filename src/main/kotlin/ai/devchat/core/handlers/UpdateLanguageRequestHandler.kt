package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.storage.DevChatSettingsState
import com.alibaba.fastjson.JSONObject

class UpdateLanguageRequestHandler(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    requestAction,
    metadata,
    payload
) {
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
