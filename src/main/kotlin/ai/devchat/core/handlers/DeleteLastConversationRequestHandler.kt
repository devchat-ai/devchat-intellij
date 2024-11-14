package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.project.Project

class DeleteLastConversationRequestHandler(project: Project, requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    project,
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.DELETE_LAST_CONVERSATION_RESPONSE
    override fun action() {
        val promptHash = payload!!.getString("hash")
        client!!.deleteLog(promptHash)
        send(payload = mapOf("command" to actionName, "hash" to promptHash))
    }
}
