package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.core.DevChatWrapper
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.project.Project

class StopGenerationRequestHandler(project: Project, requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    project,
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.STOP_GENERATION_RESPONSE

    override fun action() {
        DevChatWrapper.activeChannel?.close()
        send()
    }
}
