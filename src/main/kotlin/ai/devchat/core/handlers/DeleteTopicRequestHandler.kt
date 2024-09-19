package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.plugin.DevChatService
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.project.Project

class DeleteTopicRequestHandler(project: Project, requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    project,
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.DELETE_TOPIC_RESPONSE
    override fun action() {
        val topicHash = payload!!.getString("topicHash")
        project.getService(DevChatService::class.java).client!!.deleteTopic(topicHash)
        send(payload = mapOf("topicHash" to topicHash))
    }
}
