package ai.devchat.idea.action

import ai.devchat.common.Log.info
import ai.devchat.devchat.DevChatActionHandler
import ai.devchat.devchat.DevChatActionHandler.Companion.instance
import ai.devchat.devchat.handler.AddContextNotifyHandler
import com.alibaba.fastjson.JSONObject

class AddToDevChatAction {
    private val actionHandler: DevChatActionHandler?

    init {
        actionHandler = instance
    }

    fun execute(filePath: String, fileContent: String, language: String, startLine: Int) {
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
        val addContextNotifyHandler = AddContextNotifyHandler(actionHandler!!)
        addContextNotifyHandler.setPayload(payload)
        addContextNotifyHandler.executeAction()
    }
}
