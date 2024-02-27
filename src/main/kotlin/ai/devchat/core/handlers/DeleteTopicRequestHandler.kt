package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.storage.DevChatState
import com.alibaba.fastjson.JSONObject

class DeleteTopicRequestHandler(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.DELETE_TOPIC_RESPONSE
    override fun action() {
        val topicHash = payload!!.getString("topicHash")
        val state = DevChatState.instance
        if (!state.deletedTopicHashes.contains(topicHash)) {
            state.deletedTopicHashes += topicHash
        }
        send(payload = mapOf("topicHash" to topicHash))
    }
}
