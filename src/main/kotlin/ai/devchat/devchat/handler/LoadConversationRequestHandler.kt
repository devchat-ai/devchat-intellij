package ai.devchat.devchat.handler

import ai.devchat.devchat.BaseActionHandler
import ai.devchat.devchat.DevChatActions
import ai.devchat.idea.storage.ActiveConversation
import com.alibaba.fastjson.JSONObject

class LoadConversationRequestHandler(metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(metadata, payload) {
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
        response(payload=res)
    }

}
