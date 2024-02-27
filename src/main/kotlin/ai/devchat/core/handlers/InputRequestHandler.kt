package ai.devchat.core.handlers

import ai.devchat.core.DevChatWrapper
import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import com.alibaba.fastjson.JSONObject
import kotlinx.coroutines.runBlocking

class InputRequestHandler(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
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
