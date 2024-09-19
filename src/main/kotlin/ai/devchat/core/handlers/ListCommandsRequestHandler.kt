package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.core.DevChatClient
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.project.Project
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull


class ListCommandsRequestHandler(project: Project, requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    project,
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.LIST_COMMANDS_RESPONSE
    override fun action() {
        var client: DevChatClient? = null
        runBlocking {
            client = withTimeoutOrNull(5000) { // 5000 milliseconds = 5 seconds timeout
                while (devChatService.client == null) {
                    delay(100) // Wait for 100 milliseconds before checking again
                }
                devChatService.client
            }
        }
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
