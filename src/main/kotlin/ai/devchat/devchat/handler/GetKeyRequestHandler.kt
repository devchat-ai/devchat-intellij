package ai.devchat.devchat.handler

import ai.devchat.devchat.BaseActionHandler
import ai.devchat.devchat.DevChatActions
import ai.devchat.idea.storage.SensitiveDataStorage
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
