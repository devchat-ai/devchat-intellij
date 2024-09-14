package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DC_CLIENT
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
        val promptHash = payload!!.getString("promptHash")
        DC_CLIENT.deleteLog(promptHash)
        send(payload = mapOf("promptHash" to promptHash))
    }
}
