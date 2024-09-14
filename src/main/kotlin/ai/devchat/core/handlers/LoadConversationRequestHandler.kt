package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DC_CLIENT
import ai.devchat.core.DevChatActions
import ai.devchat.storage.ActiveConversation
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.project.Project

class LoadConversationRequestHandler(project: Project, requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    project,
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
                val logs = DC_CLIENT.getTopicLogs(topicHash)
                ActiveConversation.reset(topicHash, logs)
            }
        }
        send(payload=res)
    }

}
