package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.storage.CONFIG
import com.alibaba.fastjson.JSONObject



class GetSettingRequestHandler(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.GET_SETTING_RESPONSE
    @Suppress("UNCHECKED_CAST")
    override fun action() {
        send(payload= CONFIG.get() as? Map<String, *>)
    }
}
