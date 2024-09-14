package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.plugin.currentProject
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project

class InsertCodeRequestHandler(project: Project, requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    project,
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.INSERT_CODE_RESPONSE
    override fun action() {
        val contentText = payload!!.getString("content")
        ApplicationManager.getApplication().invokeLater {
            val editor = currentProject?.let { FileEditorManager.getInstance(it).selectedTextEditor }
            val document = editor!!.document
            val offset = editor.caretModel.offset
            CommandProcessor.getInstance().executeCommand(currentProject, {
                ApplicationManager.getApplication().runWriteAction {
                    document.insertString(offset, contentText)
                }
            }, "InsertText", null)
            send()
        }
    }

}
