package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.storage.recommendedWorkflows
import com.alibaba.fastjson.JSONObject


class ListCommandsRequestHandler(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.LIST_COMMANDS_RESPONSE
    override fun action() {
        val indexedCommands = wrapper.commandList.forEach {
            val commandName = (it as JSONObject).getString("name")
            it["recommend"] = recommendedWorkflows.indexOf(commandName)
        }
        send(payload = mapOf("commands" to indexedCommands))
    }
}
