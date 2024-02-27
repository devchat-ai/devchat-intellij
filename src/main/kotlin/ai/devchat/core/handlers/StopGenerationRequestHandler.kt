package ai.devchat.core.handlers

import ai.devchat.core.DevChatWrapper
import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import com.alibaba.fastjson.JSONObject

class StopGenerationRequestHandler(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
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
