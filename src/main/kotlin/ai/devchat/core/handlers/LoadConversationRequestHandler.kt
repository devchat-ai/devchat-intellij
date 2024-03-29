package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.storage.ActiveConversation
import com.alibaba.fastjson.JSONObject

class LoadConversationRequestHandler(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.LOAD_CONVERSATIONS_RESPONSE

    override fun action() {
        val topicHash = metadata!!.getString("topicHash")
        val res = mutableMapOf("reset" to true)
        when {
            topicHash.isNullOrEmpty() -> ActiveConversation.reset()
            topicHash == ActiveConversation.topic -> res["reset"] = false
            else -> {
                val arr = wrapper.logTopic(topicHash, null)
                // remove request_tokens and response_tokens in the conversations object
                val messages = List<JSONObject>(arr.size){i ->
                    val msg = arr.getJSONObject(i)
                    msg.remove("request_tokens")
                    msg.remove("response_tokens")
                    msg
                }
                ActiveConversation.reset(topicHash, messages)
            }
        }
        send(payload=res)
    }

}
