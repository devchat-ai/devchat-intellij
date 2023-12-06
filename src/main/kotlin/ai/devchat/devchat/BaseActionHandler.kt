package ai.devchat.devchat

import ai.devchat.cli.DevChatWrapper
import ai.devchat.common.DevChatPathUtil
import ai.devchat.common.Log
import com.alibaba.fastjson.JSONObject

const val DEFAULT_RESPONSE_FUNC = "IdeaToJSMessage"

abstract class BaseActionHandler(
    val metadata: JSONObject? = null,
    val payload: JSONObject? = null
) : ActionHandler {
    val handler = DevChatActionHandler.instance
    val wrapper = DevChatWrapper()
    val jsCallback: String = metadata?.getString("callback") ?: DEFAULT_RESPONSE_FUNC

    abstract val actionName: String

    open fun action() { response() }

    open fun except(exception: Exception) {
        response(
            metadata = mapOf(
                "status" to "error",
                "error" to exception
            )
        )
    }

    fun response(metadata: Map<String, Any?>? = null, payload: Map<String, Any?>? = null) {
        handler?.sendResponse(
            actionName,
            jsCallback,
            metadata?.let { JSONObject(it) },
            payload?.let { JSONObject(it) },
        )
    }

    override fun executeAction() {
        try {
            Log.info("Handling $actionName request")
            action()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.error("Exception occurred while handle action $actionName: ${e.message}")
            except(e)
        }
    }

}