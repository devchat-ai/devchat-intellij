package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.plugin.currentProject
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.fileEditor.FileEditorManager

class ReplaceFileContentHandler(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.REPLACE_FILE_CONTENT_RESPONSE
    override fun action() {
        val newFileContent = payload!!.getString("content")
        ApplicationManager.getApplication().invokeLater {
            val editor = FileEditorManager.getInstance(currentProject!!).selectedTextEditor
            val document = editor!!.document
            CommandProcessor.getInstance().executeCommand(currentProject, {
                ApplicationManager.getApplication().runWriteAction {
                    document.setText(newFileContent)
                } }, "ReplaceFileContentHandler", null)
            send()
        }
    }
}
