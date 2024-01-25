package ai.devchat.idea.action

import ai.devchat.common.Log.info
import ai.devchat.devchat.DevChatActions
import ai.devchat.devchat.handler.AddContextNotifyHandler
import com.alibaba.fastjson.JSONObject

class AddToDevChatAction {
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
            AddContextNotifyHandler(DevChatActions.ADD_CONTEXT_NOTIFY,null, payload).executeAction()
        }
    }

    companion object {
        var cache: JSONObject? = null
    }
}
