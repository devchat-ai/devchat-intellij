package ai.devchat.devchat.handler

import ai.devchat.common.Log
import ai.devchat.devchat.ActionHandler
import ai.devchat.devchat.DevChatActionHandler
import ai.devchat.devchat.DevChatActions
import ai.devchat.idea.setting.DevChatSettingsState
import ai.devchat.idea.storage.ActiveConversation
import com.alibaba.fastjson.JSONObject

class LoadHistoryMessagesRequestHandler(private val handler: DevChatActionHandler) : ActionHandler {
    private var metadata: JSONObject? = null
    private var payload: JSONObject? = null

    private fun action(res: JSONObject) {
        val pageSize = DevChatSettingsState.instance.maxLogCount
        val pageIndex = metadata!!.getInteger("pageIndex") ?: 1
        res["messages"] = ActiveConversation.getMessages(pageIndex, pageSize)
    }

    override fun executeAction() {
        handler.handle(
            DevChatActions.LOAD_HISTORY_MESSAGES_RESPONSE,
            metadata!!.getString("callback"),
            ::action
        )
    }

    override fun setMetadata(metadata: JSONObject) {
        this.metadata = metadata
    }

    override fun setPayload(payload: JSONObject) {
        this.payload = payload
    }
}
