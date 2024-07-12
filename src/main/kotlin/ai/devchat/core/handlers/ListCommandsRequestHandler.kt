package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DC_CLIENT
import ai.devchat.core.DevChatActions
import com.alibaba.fastjson.JSONObject


class ListCommandsRequestHandler(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.LIST_COMMANDS_RESPONSE
    override fun action() {
        val recommendedWorkflows = DC_CLIENT.getWorkflowConfig()?.recommend?.workflows.orEmpty()
        val indexedCommands = DC_CLIENT.getWorkflowList()?.map {
            val commandName = it.name
            mapOf(
                "name" to it.name,
                "namespace" to it.namespace,
                "active" to it.active,
                "command_conf" to mapOf(
                    "description" to it.commandConf.description,
                    "help" to it.commandConf.help
                ),
                "recommend" to recommendedWorkflows.indexOf(commandName)
            )
        }
        send(payload = mapOf("commands" to indexedCommands))
    }
}
