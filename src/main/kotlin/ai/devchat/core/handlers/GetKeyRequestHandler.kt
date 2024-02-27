package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.storage.SensitiveDataStorage
import com.alibaba.fastjson.JSONObject

class GetKeyRequestHandler(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.GET_KEY_RESPONSE
    override fun executeAction() {
        val key = SensitiveDataStorage.key
        if (key.isNullOrEmpty()) {
            throw RuntimeException("key is empty")
        }
        send(payload = mapOf("key" to key))
    }
}
