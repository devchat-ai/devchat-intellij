package ai.devchat.devchat.handler

import ai.devchat.devchat.BaseActionHandler
import ai.devchat.devchat.DevChatActions
import ai.devchat.idea.storage.supportedModels
import com.alibaba.fastjson.JSONObject

class ListModelsRequestHandler(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.LIST_MODELS_RESPONSE
    override fun action() {
        send(payload=mapOf("models" to supportedModels.keys.toList()))
    }
}
