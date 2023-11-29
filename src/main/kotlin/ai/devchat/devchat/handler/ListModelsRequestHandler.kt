package ai.devchat.devchat.handler

import ai.devchat.common.Log
import ai.devchat.devchat.ActionHandler
import ai.devchat.devchat.DevChatActionHandler
import ai.devchat.devchat.DevChatActions
import com.alibaba.fastjson.JSONObject

class ListModelsRequestHandler(private val devChatActionHandler: DevChatActionHandler) : ActionHandler {
    private var metadata: JSONObject? = null
    private var payload: JSONObject? = null
    override fun executeAction() {
        Log.info("Handling list model request")
        val callbackFunc = metadata!!.getString("callback")
        val modelList: MutableList<String> = ArrayList()
        modelList.add("gpt-3.5-turbo")
        modelList.add("gpt-4")
        modelList.add("gpt-3.5-turbo-16k")
        modelList.add("claude-2")
        devChatActionHandler.sendResponse(
            DevChatActions.LIST_MODELS_RESPONSE,
            callbackFunc
        ) { metadata: JSONObject, payload: JSONObject ->
            metadata["status"] = "success"
            metadata["error"] = ""
            payload["models"] = modelList
        }
    }

    override fun setMetadata(metadata: JSONObject) {
        this.metadata = metadata
    }

    override fun setPayload(payload: JSONObject) {
        this.payload = payload
    }
}
