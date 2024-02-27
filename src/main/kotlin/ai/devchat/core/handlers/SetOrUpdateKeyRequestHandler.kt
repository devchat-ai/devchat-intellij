package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.storage.SensitiveDataStorage
import com.alibaba.fastjson.JSONObject

class SetOrUpdateKeyRequestHandler(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.SET_OR_UPDATE_KEY_RESPONSE
    override fun executeAction() {
        val key = payload!!.getString("key")
        if (key == null || key.isEmpty()) {
            throw RuntimeException("key is empty")
        }
        SensitiveDataStorage.key = key
        send()
    }
}
