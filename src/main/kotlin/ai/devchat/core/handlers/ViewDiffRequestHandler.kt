package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.plugin.currentProject
import com.alibaba.fastjson.JSONObject
import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager

class ViewDiffRequestHandler(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.VIEW_DIFF_RESPONSE
    override fun action() {
        val diffContent = payload!!.getString("content")
        ApplicationManager.getApplication().invokeLater {
            val editor = FileEditorManager.getInstance(currentProject!!).selectedTextEditor
                ?: // Handle the case when no editor is opened
                return@invokeLater
            val document = editor.document
            val file = FileDocumentManager.getInstance().getFile(document)
                ?: // Handle the case when no file corresponds to the document
                return@invokeLater
            val fileType = file.fileType
            val selectionModel = editor.selectionModel
            val localContent = if (selectionModel.hasSelection()) selectionModel.selectedText else document.text
            val contentFactory = DiffContentFactory.getInstance()
            val diffRequest = SimpleDiffRequest(
                "Code Diff",
                contentFactory.create(localContent!!, fileType),
                contentFactory.create(diffContent, fileType),
                "Current Code",
                "New Code"
            )
            DiffManager.getInstance().showDiff(currentProject, diffRequest)
        }
    }
}
