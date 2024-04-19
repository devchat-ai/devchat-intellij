package ai.devchat.plugin

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ui.DialogWrapper
import java.awt.event.ActionEvent
import javax.swing.Action
import javax.swing.JComponent

class DiffViewerDialog(
    val editor: Editor,
    private val newText: String
) : DialogWrapper(editor.project) {
    init {
        super.init()
        title = "Confirm Changes"
    }

    override fun createCenterPanel(): JComponent {
        val virtualFile = FileDocumentManager.getInstance().getFile(editor.document)
        val fileType = virtualFile!!.fileType
        val localContent = if (editor.selectionModel.hasSelection()) {
            editor.selectionModel.selectedText
        } else editor.document.text
        val contentFactory = DiffContentFactory.getInstance()
        val diffRequest = SimpleDiffRequest(
            "Code Diff",
            contentFactory.create(localContent!!, fileType),
            contentFactory.create(newText, fileType),
            "Old code",
            "New code"
        )
        val diffPanel = DiffManager.getInstance().createRequestPanel(editor.project, {}, null)
        diffPanel.setRequest(diffRequest)
        return diffPanel.component
    }

    override fun createActions(): Array<Action> {

        return arrayOf(cancelAction, object: DialogWrapperAction("Apply") {
            override fun doAction(e: ActionEvent?) {
                val selectionModel = editor.selectionModel
                val document = editor.document
                val startOffset: Int?
                val endOffset: Int?
                if (selectionModel.hasSelection()) {
                    startOffset = selectionModel.selectionStart
                    endOffset = selectionModel.selectionEnd
                } else {
                    startOffset = 0
                    endOffset = document.textLength - 1
                }
                WriteCommandAction.runWriteCommandAction(editor.project) {
                    // Ensure offsets are valid
                    val safeStartOffset = startOffset.coerceIn(0, document.textLength)
                    val safeEndOffset = endOffset.coerceIn(0, document.textLength).coerceAtLeast(safeStartOffset)
                    // Replace the selected range with new text
                    document.replaceString(safeStartOffset, safeEndOffset, newText)
                }
                close(OK_EXIT_CODE)
            }
        })
    }
}