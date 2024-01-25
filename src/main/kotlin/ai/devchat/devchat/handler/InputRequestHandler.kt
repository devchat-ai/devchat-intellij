package ai.devchat.devchat.handler

import ai.devchat.cli.DevChatWrapper
import ai.devchat.devchat.BaseActionHandler
import ai.devchat.devchat.DevChatActions
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
