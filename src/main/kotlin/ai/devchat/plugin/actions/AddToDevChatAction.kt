package ai.devchat.plugin.actions

import ai.devchat.common.Log.info
import ai.devchat.core.DevChatActions
import ai.devchat.core.handlers.AddContextNotifyHandler
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.project.Project

class AddToDevChatAction(val project: Project) {
    fun execute(filePath: String, fileContent: String, language: String, startLine: Int, invokeLater: Boolean = false) {
        info(
            "Add to DevChat -> path: " + filePath +
                    " content: " + fileContent +
                    " language: " + language +
                    " startLine: " + startLine
        )
        val payload = JSONObject()
        payload["path"] = filePath
        payload["content"] = fileContent
        payload["languageId"] = language
        payload["startLine"] = startLine
        if (invokeLater) {
            cache = payload
        } else {
            AddContextNotifyHandler(project, DevChatActions.ADD_CONTEXT_NOTIFY,null, payload).executeAction()
        }
    }

    companion object {
        var cache: JSONObject? = null
    }
}
