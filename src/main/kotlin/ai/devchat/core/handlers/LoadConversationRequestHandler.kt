package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.storage.CONFIG
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.project.Project

class LoadConversationRequestHandler(project: Project, requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    project,
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.LOAD_CONVERSATIONS_RESPONSE

    override fun action() {
        val topicHash = metadata?.getString("topicHash")
        val res = mutableMapOf("reset" to true)
        when {
            topicHash.isNullOrEmpty() -> activeConversation!!.reset()
            topicHash == activeConversation!!.topic -> res["reset"] = false
            else -> {
                val logs = client!!.getTopicLogs(topicHash)
                activeConversation!!.reset(topicHash, logs)
            }
        }

        loadConversation()
    }

    fun loadConversation() {
        val pageSize = CONFIG["max_log_count"] as Int
        val pageIndex = 1
        val messages = activeConversation!!.getMessages(pageIndex, pageSize)

        // 创建新的 payload
        val newPayload = mapOf(
            "command" to "reloadMessage",
            "entries" to (messages?.map { shortLog ->
                mapOf(
                    "hash" to shortLog.hash,
                    "parent" to shortLog.parent,
                    "user" to shortLog.user,
                    "date" to shortLog.date,
                    "request" to shortLog.request,
                    "responses" to shortLog.responses,
                    "context" to shortLog.context,
                    "request_tokens" to shortLog.requestTokens,
                    "response_tokens" to shortLog.responseTokens,
                    "response" to (shortLog.responses?.joinToString("\n") ?: "")
                )
            }?.reversed() ?: emptyList()),
            "pageIndex" to 0,
            "reset" to messages.isNullOrEmpty()
        )

        send(payload = newPayload)
    }

}
