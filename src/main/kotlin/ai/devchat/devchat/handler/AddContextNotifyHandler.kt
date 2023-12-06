package ai.devchat.devchat.handler

import ai.devchat.devchat.*
import com.alibaba.fastjson.JSONObject


class AddContextNotifyHandler(metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(metadata, payload) {
    override val actionName: String = DevChatActions.ADD_CONTEXT_NOTIFY
    override fun action() {
        response(payload=mapOf(
            "path" to payload?.getString("path"),
            "content" to payload?.getString("content"),
            "languageId" to payload?.getString("languageId"),
            "startLine" to payload?.getInteger("startLine")
        ))
    }
}
