package ai.devchat.devchat.handler

import ai.devchat.cli.DevChatWrapper
import ai.devchat.common.DevChatPathUtil
import ai.devchat.common.Log
import ai.devchat.devchat.ActionHandler
import ai.devchat.devchat.DevChatActionHandler
import ai.devchat.devchat.DevChatActions
import com.alibaba.fastjson.JSONObject

class DeleteLastConversationRequestHandler(private val devChatActionHandler: DevChatActionHandler) : ActionHandler {
    private var metadata: JSONObject? = null
    private var payload: JSONObject? = null
    override fun executeAction() {
        Log.info("Handling delete last conversation request")
        val callbackFunc = metadata!!.getString("callback")
        val promptHash = payload!!.getString("promptHash")
        val flags: MutableMap<String, List<String?>> = HashMap()
        flags["delete"] = listOf(promptHash)
        val devchatCommandPath = DevChatPathUtil.devchatBinPath
        val devchatWrapper = DevChatWrapper(devchatCommandPath)
        try {
            devchatWrapper.runLogCommand(flags)
            devChatActionHandler.sendResponse(
                DevChatActions.DELETE_LAST_CONVERSATION_RESPONSE,
                callbackFunc
            ) { metadata: JSONObject, payload: JSONObject ->
                metadata["status"] = "success"
                metadata["error"] = ""
                payload["promptHash"] = promptHash
            }
        } catch (e: Exception) {
            devChatActionHandler.sendResponse(
                DevChatActions.DELETE_LAST_CONVERSATION_RESPONSE,
                callbackFunc
            ) { metadata: JSONObject, payload: JSONObject ->
                metadata["status"] = "error"
                metadata["error"] = e.message
                payload["promptHash"] = promptHash
            }
        }
    }

    override fun setMetadata(metadata: JSONObject) {
        this.metadata = metadata
    }

    override fun setPayload(payload: JSONObject) {
        this.payload = payload
    }
}
