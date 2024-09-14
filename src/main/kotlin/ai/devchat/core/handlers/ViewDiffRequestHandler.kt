package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.plugin.DiffViewerDialog
import ai.devchat.plugin.currentProject
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project

class ViewDiffRequestHandler(project: Project, requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    project,
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
            DiffViewerDialog(editor, diffContent).show()
        }
    }
}
