package ai.devchat.core.handlers

import ai.devchat.common.ProjectUtils
import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
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
        val project = ProjectUtils.project
        val newFileContent = payload!!.getString("content")
        ApplicationManager.getApplication().invokeLater {
            val editor = FileEditorManager.getInstance(project!!).selectedTextEditor
            val document = editor!!.document
            CommandProcessor.getInstance().executeCommand(project, {
                ApplicationManager.getApplication().runWriteAction {
                    document.setText(newFileContent)
                } }, "ReplaceFileContentHandler", null)
            send()
        }
    }
}
