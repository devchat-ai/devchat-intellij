package ai.devchat.core

import ai.devchat.common.Log
import ai.devchat.plugin.Browser
import ai.devchat.plugin.DevChatService
import ai.devchat.storage.ActiveConversation
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.project.Project

const val DEFAULT_RESPONSE_FUNC = "IdeaToJSMessage"

abstract class BaseActionHandler(
    val project: Project,
    val requestAction: String,
    var metadata: JSONObject? = null,
    var payload: JSONObject? = null
) : ActionHandler {
    val devChatService: DevChatService = project.getService(DevChatService::class.java)
    val client: DevChatClient? get() = devChatService.client
    val wrapper: DevChatWrapper? get() = devChatService.wrapper
    val browser: Browser? get() = devChatService.browser
    val activeConversation: ActiveConversation? get() = devChatService.activeConversation
    private val jsCallback: String = metadata?.getString("callback") ?: DEFAULT_RESPONSE_FUNC

    abstract val actionName: String

    open fun action() { send() }

    open fun except(exception: Exception) {
        send(
            metadata = mapOf(
                "status" to "error",
                "error" to exception
            )
        )
    }

    fun send(metadata: Map<String, Any?>? = null, payload: Map<String, Any?>? = null) {
        val response = JSONObject()
        response["action"] = actionName
        response["metadata"] = metadata ?: JSONObject(mapOf(
            "status" to "success",
            "error" to ""
        ))
        response["payload"] = payload ?: JSONObject()
        browser!!.sendToWebView(payload ?: JSONObject())
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