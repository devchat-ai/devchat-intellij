package ai.devchat.devchat.handler

import ai.devchat.cli.DevChatWrapper
import ai.devchat.devchat.BaseActionHandler
import ai.devchat.devchat.DevChatActions
import com.alibaba.fastjson.JSONObject

class StopGenerationRequestHandler(metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(metadata, payload) {
    override val actionName: String = DevChatActions.STOP_GENERATION_RESPONSE

    override fun action() {
        DevChatWrapper.activeChannel?.close()
        send()
    }
}
