package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.storage.SERVER_CONFIG
import com.alibaba.fastjson.JSONObject


class GetServerSettingsRequestHandler(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.GET_SERVER_SETTINGS_RESPONSE
    @Suppress("UNCHECKED_CAST")
    override fun action() {
        send(payload= SERVER_CONFIG.get() as? Map<String, *>)
    }
}
