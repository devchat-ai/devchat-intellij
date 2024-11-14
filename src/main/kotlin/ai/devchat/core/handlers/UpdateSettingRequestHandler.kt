package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.storage.CONFIG
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.project.Project

class UpdateSettingRequestHandler(project: Project, requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    project,
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.UPDATE_SETTING_RESPONSE

    override fun action() {
        CONFIG.replaceAll(payload?.getJSONObject("value")!!)
        send(payload = mapOf("command" to actionName))
    }
}