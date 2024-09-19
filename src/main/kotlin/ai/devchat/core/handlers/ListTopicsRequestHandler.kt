package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.plugin.DevChatService
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.project.Project


class ListTopicsRequestHandler(project: Project, requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    project,
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.LIST_TOPICS_RESPONSE
    override fun action() {
        val topics = project.getService(DevChatService::class.java).client!!.getTopics().map {
            val request =  it.rootPromptRequest
            val response = it.rootPromptResponse
            mapOf(
                "root_prompt" to mapOf(
                    "hash" to it.rootPromptHash,
                    "date" to it.rootPromptDate,
                    "user" to it.rootPromptUser,
                    "request" to  request,
                    "response" to response,
                    "title" to "$request-$response",
                ),
                "latest_time" to it.latestTime,
                "hidden" to it.hidden,
            )
        }
        send(payload= mapOf("topics" to topics))
    }
}