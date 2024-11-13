package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.storage.SERVER_CONFIG
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.project.Project


class GetServerSettingsRequestHandler(project: Project, requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    project,
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.GET_SERVER_SETTINGS_RESPONSE
    @Suppress("UNCHECKED_CAST")
    override fun action() {
        send(payload= mapOf( "command" to actionName, "value" to SERVER_CONFIG.get() as? Map<String, *>))
    }
}
