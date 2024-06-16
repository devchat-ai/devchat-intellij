package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.plugin.ideServerPort
import com.alibaba.fastjson.JSONObject



class GetIDEServicePortRequestHandler(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.GET_IDE_SERVICE_PORT_RESPONSE
    override fun action() {
        send(payload= mapOf("result" to ideServerPort))
    }
}
