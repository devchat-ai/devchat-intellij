package ai.devchat.devchat.handler

import ai.devchat.common.Log
import ai.devchat.devchat.ActionHandler
import ai.devchat.devchat.DevChatActionHandler
import ai.devchat.devchat.DevChatActions
import ai.devchat.idea.storage.SensitiveDataStorage
import com.alibaba.fastjson.JSONObject

class SetOrUpdateKeyRequestHandler(private val devChatActionHandler: DevChatActionHandler) : ActionHandler {
    private var metadata: JSONObject? = null
    private var payload: JSONObject? = null
    override fun executeAction() {
        Log.info("Handling set or update key request")
        val key = payload!!.getString("key")
        val callbackFunc = metadata!!.getString("callback")
        if (key == null || key.isEmpty()) {
            Log.error("Key is empty")
            devChatActionHandler.sendResponse(
                DevChatActions.SET_OR_UPDATE_KEY_RESPONSE,
                callbackFunc
            ) { metadata: JSONObject, payload: JSONObject? ->
                metadata["status"] = "error"
                metadata["error"] = "key is empty"
            }
        } else {
            SensitiveDataStorage.setKey(key)
            devChatActionHandler.sendResponse(
                DevChatActions.SET_OR_UPDATE_KEY_RESPONSE,
                callbackFunc
            ) { metadata: JSONObject, payload: JSONObject? ->
                metadata["status"] = "success"
                metadata["error"] = ""
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
