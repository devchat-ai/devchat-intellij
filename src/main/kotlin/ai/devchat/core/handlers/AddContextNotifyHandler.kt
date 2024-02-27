package ai.devchat.core.handlers

import ai.devchat.core.*
import com.alibaba.fastjson.JSONObject


class AddContextNotifyHandler(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.ADD_CONTEXT_NOTIFY
    override fun action() {
        send(payload=mapOf(
            "path" to payload?.getString("path"),
            "content" to payload?.getString("content"),
            "languageId" to payload?.getString("languageId"),
            "startLine" to payload?.getInteger("startLine")
        ))
    }
}
