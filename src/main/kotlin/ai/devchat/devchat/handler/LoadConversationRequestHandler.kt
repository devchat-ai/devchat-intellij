package ai.devchat.devchat.handler

import ai.devchat.common.Log
import ai.devchat.devchat.ActionHandler
import ai.devchat.devchat.DevChatActionHandler
import ai.devchat.devchat.DevChatActions
import com.alibaba.fastjson.JSONObject

class LoadConversationRequestHandler(private val handler: DevChatActionHandler) : ActionHandler {
    private var metadata: JSONObject? = null
    private var payload: JSONObject? = null

    private fun action(resMetadata: JSONObject, resPayload: JSONObject) {
        val topicHash = metadata!!.getString("topicHash")
        val conversations = handler.devChat.logTopic(topicHash, null)
        // remove request_tokens and response_tokens in the conversations object
        for (i in conversations.indices) {
            val conversation = conversations.getJSONObject(i)
            conversation.remove("request_tokens")
            conversation.remove("response_tokens")
        }
        resMetadata["status"] = "success"
        resMetadata["error"] = ""
        resPayload["conversations"] = conversations
    }

    override fun executeAction() {
        handler.handle(
            DevChatActions.LOAD_CONVERSATIONS_RESPONSE,
            metadata!!.getString("callback"),
        ) {resMetadata: JSONObject, resPayload: JSONObject ->
            action(resMetadata, resPayload)
        }
    }

    override fun setMetadata(metadata: JSONObject) {
        this.metadata = metadata
    }

    override fun setPayload(payload: JSONObject) {
        this.payload = payload
    }
}
