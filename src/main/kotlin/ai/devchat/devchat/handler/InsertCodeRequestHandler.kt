package ai.devchat.devchat.handler

import ai.devchat.devchat.BaseActionHandler
import ai.devchat.devchat.DevChatActions
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.fileEditor.FileEditorManager

class InsertCodeRequestHandler(metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(metadata, payload) {
    override val actionName: String = DevChatActions.INSERT_CODE_RESPONSE
    override fun action() {
        val project = handler?.project
        val contentText = payload!!.getString("content")
        ApplicationManager.getApplication().invokeLater {
            val editor = project?.let { FileEditorManager.getInstance(it).selectedTextEditor }
            val document = editor!!.document
            val offset = editor.caretModel.offset
            CommandProcessor.getInstance().executeCommand(project, {
                ApplicationManager.getApplication().runWriteAction {
                    document.insertString(offset, contentText)
                }
            }, "InsertText", null)
            send()
        }
    }

}
