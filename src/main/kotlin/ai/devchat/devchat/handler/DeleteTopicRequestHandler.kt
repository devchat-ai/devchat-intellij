package ai.devchat.devchat.handler

import ai.devchat.devchat.ActionHandler
import ai.devchat.devchat.DevChatActionHandler
import ai.devchat.devchat.DevChatActions
import ai.devchat.idea.storage.DeletedTopicsState
import com.alibaba.fastjson.JSONObject

class DeleteTopicRequestHandler(private val devChatActionHandler: DevChatActionHandler) : ActionHandler {
    private var metadata: JSONObject? = null
    private var payload: JSONObject? = null
    override fun executeAction() {
        val callbackFunc = metadata!!.getString("callback")
        val topicHash = payload!!.getString("topicHash")
        val state = DeletedTopicsState.instance
        if (!state.deletedTopicHashes.contains(topicHash)) {
            state.deletedTopicHashes += topicHash
        }
        devChatActionHandler.sendResponse(
            DevChatActions.DELETE_TOPIC_RESPONSE,
            callbackFunc
        ) { metadata: JSONObject, payload: JSONObject ->
            metadata["status"] = "success"
            metadata["error"] = ""
            payload["topicHash"] = topicHash
        }
    }

    override fun setMetadata(metadata: JSONObject) {
        this.metadata = metadata
    }

    override fun setPayload(payload: JSONObject) {
        this.payload = payload
    }
}
