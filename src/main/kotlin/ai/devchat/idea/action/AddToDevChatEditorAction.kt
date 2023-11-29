package ai.devchat.idea.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class AddToDevChatEditorAction : AnAction() {
    private val addToDevChatAction: AddToDevChatAction

    init {
        addToDevChatAction = AddToDevChatAction()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.setEnabled(true)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)
        val project = e.project
        val projectPath = project!!.basePath
        val absoluteFilePath = virtualFile!!.path
        var relativePath = absoluteFilePath
        if (projectPath != null && absoluteFilePath.startsWith(projectPath)) {
            relativePath = absoluteFilePath.substring(projectPath.length + 1)
        }
        val fileType = virtualFile.fileType
        val language = fileType.name
        if (editor != null) {
            val selectionModel = editor.selectionModel
            var selectedText = selectionModel.selectedText
            if (selectedText == null || selectedText.isEmpty()) {
                val document = editor.document
                selectedText = document.text
            }
            val startOffset = selectionModel.selectionStart
            val document = editor.document
            val startLine = document.getLineNumber(startOffset) + 1
            addToDevChatAction.execute(relativePath, selectedText, language, startLine)
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
