package ai.devchat.devchat.handler

import ai.devchat.devchat.ActionHandler
import ai.devchat.devchat.DevChatActionHandler
import ai.devchat.devchat.DevChatActions
import ai.devchat.idea.settings.DevChatSettingsState
import ai.devchat.idea.storage.ActiveConversation
import com.alibaba.fastjson.JSONObject
import org.jvnet.staxex.StAxSOAPBody.Payload

class LoadHistoryMessagesRequestHandler(private val handler: DevChatActionHandler) : ActionHandler {
    private var metadata: JSONObject? = null
    private var payload: JSONObject? = null

    private fun success(resMetadata: JSONObject, resPayload: JSONObject) {
        val pageSize = DevChatSettingsState.instance.maxLogCount
        val pageIndex = metadata!!.getInteger("pageIndex") ?: 1
        resPayload["messages"] = ActiveConversation.getMessages(pageIndex, pageSize)
    }

    override fun executeAction() {
        handler.handle(
            DevChatActions.LOAD_HISTORY_MESSAGES_RESPONSE,
            metadata!!.getString("callback"),
            ::success,
        ) { _, _ -> }
    }

    override fun setMetadata(metadata: JSONObject) {
        this.metadata = metadata
    }

    override fun setPayload(payload: JSONObject) {
        this.payload = payload
    }
}
