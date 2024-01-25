package ai.devchat.devchat.handler

import ai.devchat.devchat.*
import com.alibaba.fastjson.JSONObject


class SendUserMessageHandler(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
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
}
