package ai.devchat.devchat

import ai.devchat.cli.DevChatWrapper
import ai.devchat.common.DevChatPathUtil
import ai.devchat.common.Log
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser

/**
 * DevChatActionHandler class uses singleton pattern.
 */
class DevChatActionHandler private constructor() {
    val devChat: DevChatWrapper = DevChatWrapper(DevChatPathUtil.devchatBinPath)
    private var cefBrowser: CefBrowser? = null
    var project: Project? = null
        private set

    fun initialize(cefBrowser: CefBrowser, project: Project?) {
        this.cefBrowser = cefBrowser
        this.project = project
    }
    fun handle(
        action: String,
        jsCallback: String,
        callback: (JSONObject, JSONObject) -> Unit,
    ) {
        try {
            Log.info("Handling $action request")
            sendResponse(action, jsCallback, callback)
        } catch (e: Exception) {
            Log.error("Exception occurred while handle action $action: ${e.message}")
            sendResponse(action, jsCallback) { metadata: JSONObject, _ ->
                metadata["status"] = "error"
                metadata["error"] = e.message
            }
        }
    }

    fun sendResponse(action: String, responseFunc: String, callback: (JSONObject, JSONObject) -> Unit) {
        val response = JSONObject()
        response["action"] = action
        val metadata = JSONObject()
        val payload = JSONObject()
        response["metadata"] = metadata
        response["payload"] = payload
        callback(metadata, payload)
        cefBrowser!!.executeJavaScript("$responseFunc($response)", "", 0)
    }

    companion object {
        @JvmStatic
        @get:Synchronized
        var instance: DevChatActionHandler? = null
            get() {
                if (field == null) {
                    field = DevChatActionHandler()
                }
                return field
            }
            private set
    }
}
