package ai.devchat.devchat.handler

import ai.devchat.devchat.ActionHandler
import ai.devchat.devchat.DevChatActionHandler
import ai.devchat.devchat.DevChatActions
import com.alibaba.fastjson.JSONObject

class AddContextNotifyHandler(private val devChatActionHandler: DevChatActionHandler) : ActionHandler {
    private var metadata: JSONObject? = null
    private var payload: JSONObject? = null
    val RESPONSE_FUNC = "IdeaToJSMessage"
    override fun executeAction() {
        devChatActionHandler.sendResponse(
            DevChatActions.ADD_CONTEXT_NOTIFY,
            RESPONSE_FUNC
        ) { metadata: JSONObject, payload: JSONObject ->
            metadata["status"] = "success"
            metadata["error"] = ""
            payload["path"] = this.payload!!.getString("path")
            payload["content"] = this.payload!!.getString("content")
            payload["languageId"] = this.payload!!.getString("languageId")
            payload["startLine"] = this.payload!!.getInteger("startLine")
        }
    }

    override fun setMetadata(metadata: JSONObject) {
        this.metadata = metadata
    }

    override fun setPayload(payload: JSONObject) {
        this.payload = payload
    }
}
