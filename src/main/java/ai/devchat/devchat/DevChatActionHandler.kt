package ai.devchat.devchat

import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import java.util.function.BiConsumer

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

    fun sendResponse(action: String, responseFunc: String, callback: BiConsumer<JSONObject, JSONObject>) {
        val response = JSONObject()
        response["action"] = action
        val metadata = JSONObject()
        val payload = JSONObject()
        response["metadata"] = metadata
        response["payload"] = payload
        callback.accept(metadata, payload)
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
