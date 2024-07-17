package ai.devchat.plugin

import ai.devchat.common.CommandLine
import ai.devchat.common.Log
import ai.devchat.common.PathUtils
import ai.devchat.core.DevChatActions
import ai.devchat.core.handlers.CodeDiffApplyHandler
import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ui.DialogWrapper
import java.awt.event.ActionEvent
import java.io.File
import java.nio.file.Paths
import javax.swing.Action
import javax.swing.JComponent

class DiffViewerDialog(
    val editor: Editor,
    private var newText: String,
    autoEdit: Boolean = false
) : DialogWrapper(editor.project) {
    private var startOffset: Int = 0
    private var endOffset: Int = editor.document.textLength
    private var localContent: String = editor.document.text

    init {
        title = "Confirm Changes"
        val selectionModel = editor.selectionModel
        val maxIdx = editor.document.textLength
        if (!autoEdit && selectionModel.hasSelection()) {
            startOffset = selectionModel.selectionStart.coerceIn(0, maxIdx)
            endOffset = selectionModel.selectionEnd.coerceIn(0, maxIdx).coerceAtLeast(startOffset)
            localContent = editor.selectionModel.selectedText ?: ""
        }
        if (autoEdit) {
            try {
                newText = editText()
            } catch(e: Exception) {
                Log.warn("Failed to edit code: $e")
            }
        }
        super.init()
    }

    override fun createCenterPanel(): JComponent {
        val fileType = FileDocumentManager.getInstance().getFile(editor.document)!!.fileType
        val contentFactory = DiffContentFactory.getInstance()
        val diffRequest = SimpleDiffRequest(
            "Code Diff",
            contentFactory.create(localContent, fileType),
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
                WriteCommandAction.runWriteCommandAction(editor.project) {
                    editor.document.replaceString(startOffset, endOffset, newText)
                }
                CodeDiffApplyHandler(DevChatActions.CODE_DIFF_APPLY_REQUEST,null, null).executeAction()
                close(OK_EXIT_CODE)
            }
        })
    }

    private fun editText(): String {
        val srcTempFile = PathUtils.createTempFile(editor.document.text, "code_editor_src_")
        val newTempFile = PathUtils.createTempFile(newText, "code_editor_new_")
        val resultTempFile = PathUtils.createTempFile("", "code_editor_res_")
        val codeEditorPath = Paths.get(PathUtils.toolsPath, PathUtils.codeEditorBinary).toString()
        val result = CommandLine.exec(codeEditorPath, srcTempFile!!, newTempFile!!, resultTempFile!!)
        require(result.exitCode == 0) {
            throw Exception("Code editor failed with exit code ${result.exitCode}")
        }
        return File(resultTempFile).readText()
    }
}