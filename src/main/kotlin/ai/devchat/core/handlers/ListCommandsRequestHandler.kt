package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.project.Project


class ListCommandsRequestHandler(project: Project, requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    project,
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.LIST_COMMANDS_RESPONSE
    override fun action() {
        val recommendedWorkflows = client?.getWorkflowConfig()?.recommend?.workflows.orEmpty()
        val indexedCommands = client?.getWorkflowList()?.map {
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
        }.orEmpty()
        send(payload = mapOf("commands" to indexedCommands))
    }
}
