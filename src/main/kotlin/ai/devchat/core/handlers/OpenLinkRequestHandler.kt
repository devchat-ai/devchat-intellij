package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import com.alibaba.fastjson.JSONObject
import com.intellij.ide.BrowserUtil
import java.net.URL

class OpenLinkRequestHandler(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.OPEN_LINK_RESPONSE

    override fun action() {
        val url = payload!!.getString("url")
        BrowserUtil.browse(URL(url))
        send()
    }
}
