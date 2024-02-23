package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.storage.CONFIG
import com.alibaba.fastjson.JSONObject

class UpdateSettingRequestHandler(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.UPDATE_SETTING_RESPONSE

    override fun action() {
        payload!!.getJSONObject("setting")
            .getString("currentModel")?.let {
                CONFIG["default_model"] = it
            }
        send()
    }
}
