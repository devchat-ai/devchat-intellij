package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.project.Project


class AddContextNotifyHandler(project: Project, requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    project,
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.ADD_CONTEXT_NOTIFY

    override fun action() {
        val contextObj = mapOf(
            "path" to (payload?.getString("path") ?: ""),
            "content" to (payload?.getString("content") ?: ""),
            "command" to ""
        )

        val newPayload = mapOf(
            "command" to actionName,
            "file" to (payload?.getString("path") ?: ""),
            "result" to JSONObject.toJSONString(contextObj)
        )

        send(payload = newPayload)
    }
}
