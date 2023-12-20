package ai.devchat.common

import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser

object ProjectUtils {
    var cefBrowser: CefBrowser? = null
    var project: Project? = null

    fun executeJS(func: String, arg: Any? = null) {
        val funcCall = if (arg == null) "$func()" else "$func($arg)"
        cefBrowser!!.executeJavaScript(funcCall, "", 0)
    }
}
