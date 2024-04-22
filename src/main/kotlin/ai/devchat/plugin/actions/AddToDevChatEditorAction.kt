package ai.devchat.plugin.actions

import ai.devchat.common.DevChatBundle
import ai.devchat.plugin.DevChatToolWindow
import ai.devchat.storage.CONFIG
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.wm.ToolWindowManager

class AddToDevChatEditorAction : AnAction() {
    private val addToDevChatAction: AddToDevChatAction = AddToDevChatAction()

    override fun update(e: AnActionEvent) {
        e.presentation.setEnabled(true)
        if ((CONFIG["language"] as? String) == "zh") {
            e.presentation.text = DevChatBundle.message("action.addToDevChat.text.zh")
        }
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
            ToolWindowManager.getInstance(project).getToolWindow("DevChat")?.show {
                val selectionModel = editor.selectionModel
                var selectedText = selectionModel.selectedText
                if (selectedText.isNullOrEmpty()) {
                    val document = editor.document
                    selectedText = document.text
                }
                val startOffset = selectionModel.selectionStart
                val document = editor.document
                val startLine = document.getLineNumber(startOffset) + 1
                addToDevChatAction.execute(relativePath, selectedText, language, startLine, !DevChatToolWindow.loaded)
            }
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
