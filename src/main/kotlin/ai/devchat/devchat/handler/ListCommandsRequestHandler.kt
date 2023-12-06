package ai.devchat.devchat.handler

import ai.devchat.devchat.BaseActionHandler
import ai.devchat.devchat.DevChatActions
import com.alibaba.fastjson.JSONObject


class ListCommandsRequestHandler(metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(metadata, payload) {
    override val actionName: String = DevChatActions.LIST_COMMANDS_RESPONSE
    override fun action() {
        send(payload= mapOf("commands" to wrapper.commandList))
    }
}
