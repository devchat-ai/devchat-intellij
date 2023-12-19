package ai.devchat.devchat

import ai.devchat.common.Log
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser

/**
 * DevChatActionHandler class uses singleton pattern.
 */
class DevChatActionHandler private constructor() {
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
        success: (JSONObject, JSONObject) -> Unit,
        fail: (JSONObject, JSONObject) -> Unit,
    ) {
        val response = JSONObject()
        response["action"] = action
        val metadata = JSONObject()
        val payload = JSONObject()
        response["metadata"] = metadata
        response["payload"] = payload

        try {
            Log.info("Handling $action request")
            metadata["status"] = "success"
            metadata["error"] = ""
            success(metadata, payload)
        } catch (e: Exception) {
            Log.error("Exception occurred while handle action $action: ${e.message}")
            metadata["status"] = "error"
            metadata["error"] = e.message
            fail(metadata, payload)
        }
        cefBrowser!!.executeJavaScript("$jsCallback($response)", "", 0)
    }

    fun sendResponse(
        action: String,
        jsCallback: String,
        metadata: JSONObject? = null,
        payload: JSONObject? = null,
    ) {
        val response = JSONObject()
        response["action"] = action
        response["metadata"] = metadata ?: JSONObject(mapOf(
            "status" to "success",
            "error" to ""
        ))
        response["payload"] = payload ?: JSONObject()
        cefBrowser!!.executeJavaScript("$jsCallback($response)", "", 0)
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
