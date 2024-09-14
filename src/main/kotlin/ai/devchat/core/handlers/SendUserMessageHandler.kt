package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.project.Project


class SendUserMessageHandler(project: Project, requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    project,
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.SEND_USER_MESSAGE_RESPONSE
    override fun action() {
        send(payload=mapOf(
            "command" to payload?.getString("command"),
            "message" to payload?.getString("message"),
        ))
    }

    companion object {
        var cache: JSONObject? = null
    }
}
