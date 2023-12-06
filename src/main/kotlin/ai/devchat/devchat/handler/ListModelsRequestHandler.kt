package ai.devchat.devchat.handler

import ai.devchat.common.Log
import ai.devchat.devchat.ActionHandler
import ai.devchat.devchat.DevChatActionHandler
import ai.devchat.devchat.DevChatActions
import ai.devchat.idea.settings.supportedModels
import com.alibaba.fastjson.JSONObject

class ListModelsRequestHandler(private val devChatActionHandler: DevChatActionHandler) : ActionHandler {
    private var metadata: JSONObject? = null
    private var payload: JSONObject? = null
    override fun executeAction() {
        Log.info("Handling list model request")
        val callbackFunc = metadata!!.getString("callback")
        devChatActionHandler.sendResponse(
            DevChatActions.LIST_MODELS_RESPONSE,
            callbackFunc
        ) { metadata: JSONObject, payload: JSONObject ->
            metadata["status"] = "success"
            metadata["error"] = ""
            payload["models"] = supportedModels.toList()
        }
    }

    override fun setMetadata(metadata: JSONObject) {
        this.metadata = metadata
    }

    override fun setPayload(payload: JSONObject) {
        this.payload = payload
    }
}
