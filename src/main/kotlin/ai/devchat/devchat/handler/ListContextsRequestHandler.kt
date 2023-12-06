package ai.devchat.devchat.handler

import ai.devchat.devchat.BaseActionHandler
import ai.devchat.devchat.DevChatActions
import com.alibaba.fastjson.JSONObject


class ListContextsRequestHandler(metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(metadata, payload) {
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
