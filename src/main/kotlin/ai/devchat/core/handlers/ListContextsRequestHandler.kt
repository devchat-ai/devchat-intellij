package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import com.alibaba.fastjson.JSONObject


class ListContextsRequestHandler(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.LIST_CONTEXTS_RESPONSE
    override fun action() {
        val contexts = listOf(
            mapOf(
                "command" to "git diff -cached",
                "description" to "the staged changes since the last commit"
            ),
            mapOf(
                "command" to "git diff HEAD",
                "description" to "all changes since the last commit"
            )
        )
        send(payload=mapOf("contexts" to contexts))
    }

}
