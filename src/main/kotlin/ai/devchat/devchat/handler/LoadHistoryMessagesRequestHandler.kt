package ai.devchat.devchat.handler

import ai.devchat.devchat.BaseActionHandler
import ai.devchat.devchat.DevChatActions
import ai.devchat.idea.settings.DevChatSettingsState
import ai.devchat.idea.storage.ActiveConversation
import com.alibaba.fastjson.JSONObject

class LoadHistoryMessagesRequestHandler(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.LOAD_HISTORY_MESSAGES_RESPONSE

    override fun action() {
        val pageSize = DevChatSettingsState.instance.maxLogCount
        val pageIndex = metadata!!.getInteger("pageIndex") ?: 1
        val messages = ActiveConversation.getMessages(pageIndex, pageSize)
        send(payload = mapOf("messages" to messages))
    }
}
