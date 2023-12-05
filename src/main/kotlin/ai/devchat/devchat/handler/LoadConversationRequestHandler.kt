package ai.devchat.devchat.handler

import ai.devchat.common.Log
import ai.devchat.devchat.ActionHandler
import ai.devchat.devchat.DevChatActionHandler
import ai.devchat.devchat.DevChatActions
import ai.devchat.idea.storage.ActiveConversation
import com.alibaba.fastjson.JSONObject

class LoadConversationRequestHandler(private val handler: DevChatActionHandler) : ActionHandler {
    private var metadata: JSONObject? = null
    private var payload: JSONObject? = null

    private fun action(res: JSONObject) {
        val topicHash = metadata!!.getString("topicHash")
        res["reset"] = true
        when {
            topicHash.isNullOrEmpty() -> ActiveConversation.reset()
            topicHash == ActiveConversation.topic -> res["reset"] = false
            else -> {
                val arr = handler.devChat.logTopic(topicHash, null)
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
    }

    override fun executeAction() {
        handler.handle(
            DevChatActions.LOAD_CONVERSATIONS_RESPONSE,
            metadata!!.getString("callback"),
            ::action
        )
    }

    override fun setMetadata(metadata: JSONObject) {
        this.metadata = metadata
    }

    override fun setPayload(payload: JSONObject) {
        this.payload = payload
    }
}
