package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import com.alibaba.fastjson.JSONObject


class ListCommandsRequestHandler(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.LIST_COMMANDS_RESPONSE
    override fun action() {
        val recommendedWorkflows = wrapper.recommendedCommands
        val indexedCommands = wrapper.commandList.map {
            val commandName = (it as JSONObject).getString("name")
            it["recommend"] = recommendedWorkflows.indexOf(commandName)
            it
        }
        send(payload = mapOf("commands" to indexedCommands))
    }
}
