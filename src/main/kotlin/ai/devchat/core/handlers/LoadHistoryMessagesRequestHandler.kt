package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.storage.ActiveConversation
import ai.devchat.storage.CONFIG
import com.alibaba.fastjson.JSONObject

class LoadHistoryMessagesRequestHandler(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.LOAD_HISTORY_MESSAGES_RESPONSE

    override fun action() {
        val pageSize = CONFIG["max_log_count"] as Int
        val pageIndex = metadata!!.getInteger("pageIndex") ?: 1
        val messages = ActiveConversation.getMessages(pageIndex, pageSize)
        send(payload = mapOf("messages" to messages))
    }
}
