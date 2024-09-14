package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.core.DevChatWrapper
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking

class InputRequestHandler(project: Project, requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    project,
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.INPUT_RESPONSE

    override fun action() {
        runBlocking {
            DevChatWrapper.activeChannel?.send(payload!!.getString("data"))
        }
        send()
    }
}
