package ai.devchat.devchat.handler

import ai.devchat.devchat.BaseActionHandler
import ai.devchat.devchat.DevChatActions
import ai.devchat.idea.storage.DeletedTopicsState
import com.alibaba.fastjson.JSONObject

class DeleteTopicRequestHandler(metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(metadata, payload) {
    override val actionName: String = DevChatActions.DELETE_TOPIC_RESPONSE
    override fun action() {
        val topicHash = payload!!.getString("topicHash")
        val state = DeletedTopicsState.instance
        if (!state.deletedTopicHashes.contains(topicHash)) {
            state.deletedTopicHashes += topicHash
        }
        send(payload = mapOf("topicHash" to topicHash))
    }
}
