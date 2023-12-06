package ai.devchat.devchat.handler

import ai.devchat.devchat.ActionHandler
import ai.devchat.devchat.DevChatActionHandler
import ai.devchat.devchat.DevChatActions
import com.alibaba.fastjson.JSONObject
import com.intellij.ide.BrowserUtil
import java.net.URL

class OpenLinkRequestHandler(private val handler: DevChatActionHandler) : ActionHandler {
    private var metadata: JSONObject? = null
    private var payload: JSONObject? = null

    private fun action(res: JSONObject) {
        val url = payload!!.getString("url")
        BrowserUtil.browse(URL(url))
    }

    override fun executeAction() {
        handler.handle(
            DevChatActions.OPEN_LINK_RESPONSE,
            metadata!!.getString("callback"),
            ::action
        )
    }

    override fun setMetadata(metadata: JSONObject) {
        this.metadata = metadata
    }

    override fun setPayload(payload: JSONObject) {
        this.payload = payload
    }
}
