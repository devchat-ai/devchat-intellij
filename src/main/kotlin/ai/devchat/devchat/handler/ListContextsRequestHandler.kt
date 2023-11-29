package ai.devchat.devchat.handler

import ai.devchat.common.Log
import ai.devchat.devchat.ActionHandler
import ai.devchat.devchat.DevChatActionHandler
import ai.devchat.devchat.DevChatActions
import com.alibaba.fastjson.JSONObject

class ListContextsRequestHandler(private val devChatActionHandler: DevChatActionHandler) : ActionHandler {
    private var metadata: JSONObject? = null
    private var payload: JSONObject? = null
    override fun executeAction() {
        Log.info("Handling list context request")
        val callbackFunc = metadata!!.getString("callback")
        val contextList: MutableList<Map<String, String>> = ArrayList()
        val context1: MutableMap<String, String> = HashMap()
        context1["command"] = "git diff -cached"
        context1["description"] = "the staged changes since the last commit"
        val context2: MutableMap<String, String> = HashMap()
        context2["command"] = "git diff HEAD"
        context2["description"] = "all changes since the last commit"
        contextList.add(context1)
        contextList.add(context2)
        devChatActionHandler.sendResponse(
            DevChatActions.LIST_CONTEXTS_RESPONSE,
            callbackFunc
        ) { metadata: JSONObject, payload: JSONObject ->
            metadata["status"] = "success"
            metadata["error"] = ""
            payload["contexts"] = contextList
        }
    }

    override fun setMetadata(metadata: JSONObject) {
        this.metadata = metadata
    }

    override fun setPayload(payload: JSONObject) {
        this.payload = payload
    }
}
