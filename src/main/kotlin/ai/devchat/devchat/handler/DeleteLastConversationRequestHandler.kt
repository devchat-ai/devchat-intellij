package ai.devchat.devchat.handler

import ai.devchat.devchat.BaseActionHandler
import ai.devchat.devchat.DevChatActions
import com.alibaba.fastjson.JSONObject

class DeleteLastConversationRequestHandler(metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(metadata, payload) {
    override val actionName: String = DevChatActions.DELETE_LAST_CONVERSATION_RESPONSE
    override fun action() {
        val promptHash = payload!!.getString("promptHash")
        wrapper.log(mutableListOf("delete" to promptHash), null)
        send(payload = mapOf("promptHash" to promptHash))
    }
}
