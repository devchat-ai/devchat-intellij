package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.storage.SERVER_CONFIG
import com.alibaba.fastjson.JSONObject

class UpdateServerSettingsRequestHandler(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.UPDATE_SERVER_SETTINGS_RESPONSE

    override fun action() {
        SERVER_CONFIG.replaceAll(payload!!)
        send()
    }
}
