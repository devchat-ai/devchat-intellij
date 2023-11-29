package ai.devchat.devchat

import com.alibaba.fastjson.JSONObject

interface ActionHandler {
    fun setMetadata(metadata: JSONObject?)
    fun setPayload(payload: JSONObject?)
    fun executeAction()
}
