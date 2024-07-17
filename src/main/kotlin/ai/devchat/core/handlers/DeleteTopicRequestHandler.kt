package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DC_CLIENT
import ai.devchat.core.DevChatActions
import com.alibaba.fastjson.JSONObject

class DeleteTopicRequestHandler(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.DELETE_TOPIC_RESPONSE
    override fun action() {
        val topicHash = payload!!.getString("topicHash")
        DC_CLIENT.deleteTopic(topicHash)
        send(payload = mapOf("topicHash" to topicHash))
    }
}
