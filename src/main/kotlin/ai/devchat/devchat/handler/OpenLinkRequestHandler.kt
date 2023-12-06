package ai.devchat.devchat.handler

import ai.devchat.devchat.BaseActionHandler
import ai.devchat.devchat.DevChatActions
import com.alibaba.fastjson.JSONObject
import com.intellij.ide.BrowserUtil
import java.net.URL

class OpenLinkRequestHandler(metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(metadata, payload) {
    override val actionName: String = DevChatActions.OPEN_LINK_RESPONSE

    override fun action() {
        val url = payload!!.getString("url")
        BrowserUtil.browse(URL(url))
        send()
    }
}
