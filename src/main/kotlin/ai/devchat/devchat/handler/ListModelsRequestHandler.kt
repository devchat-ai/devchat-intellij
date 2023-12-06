package ai.devchat.devchat.handler

import ai.devchat.devchat.BaseActionHandler
import ai.devchat.devchat.DevChatActions
import ai.devchat.idea.settings.supportedModels
import com.alibaba.fastjson.JSONObject

class ListModelsRequestHandler(metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(metadata, payload) {
    override val actionName: String = DevChatActions.LIST_MODELS_RESPONSE
    override fun action() {
        response(payload=mapOf("models" to supportedModels.toList()))
    }
}
