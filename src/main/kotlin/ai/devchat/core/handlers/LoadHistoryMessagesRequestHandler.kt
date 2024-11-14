package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.core.ShortLog
import ai.devchat.storage.CONFIG
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.project.Project

class LoadHistoryMessagesRequestHandler(project: Project, requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    project,
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.LOAD_HISTORY_MESSAGES_RESPONSE

    override fun action() {
        val pageSize = CONFIG["max_log_count"] as Int
        val pageIndex = metadata!!.getInteger("page") ?: 1
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
