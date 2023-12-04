package ai.devchat.devchat.handler

import ai.devchat.cli.DevChatWrapper
import ai.devchat.common.DevChatPathUtil
import ai.devchat.common.Log
import ai.devchat.devchat.ActionHandler
import ai.devchat.devchat.DevChatActionHandler
import ai.devchat.devchat.DevChatActions
import com.alibaba.fastjson.JSONObject

class ListConversationsRequestHandler(private val devChatActionHandler: DevChatActionHandler) : ActionHandler {
    private var metadata: JSONObject? = null
    private var payload: JSONObject? = null
    override fun executeAction() {
        Log.info("Handling list conversations request")
        val callbackFunc = metadata!!.getString("callback")
        val topicHash = metadata!!.getString("topicHash")
        try {
            val devchatWrapper = DevChatWrapper(DevChatPathUtil.devchatBinPath)
            val conversations = devchatWrapper.listConversationsInOneTopic(topicHash)
            // remove request_tokens and response_tokens in the conversations object
            for (i in conversations.indices) {
                val conversation = conversations.getJSONObject(i)
                conversation.remove("request_tokens")
                conversation.remove("response_tokens")
            }
            devChatActionHandler.sendResponse(
                DevChatActions.LIST_CONVERSATIONS_RESPONSE,
                callbackFunc
            ) { metadata: JSONObject, payload: JSONObject ->
                metadata["status"] = "success"
                metadata["error"] = ""
                payload["conversations"] = conversations
            }
        } catch (e: Exception) {
            Log.error("Exception occrred while executing DevChat command. Exception message: " + e.message)
            devChatActionHandler.sendResponse(
                DevChatActions.LIST_CONVERSATIONS_RESPONSE,
                callbackFunc
            ) { metadata: JSONObject, payload: JSONObject? ->
                metadata["status"] = "error"
                metadata["error"] = e.message
            }
        }
    }

    override fun setMetadata(metadata: JSONObject) {
        this.metadata = metadata
    }

    override fun setPayload(payload: JSONObject) {
        this.payload = payload
    }
}
