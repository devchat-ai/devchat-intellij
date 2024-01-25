package ai.devchat.devchat.handler

import ai.devchat.devchat.BaseActionHandler
import ai.devchat.devchat.DevChatActions
import ai.devchat.idea.storage.SensitiveDataStorage
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
