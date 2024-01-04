package ai.devchat.idea

import com.intellij.codeInsight.codeVision.*
import com.intellij.codeInsight.codeVision.CodeVisionState.Companion.READY_EMPTY
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.codeInsight.hints.*
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.elementType
import java.awt.event.MouseEvent
import java.lang.Integer.min
import javax.swing.JComponent

@Suppress("UnstableApiUsage")
class GenTestsCodeVisionProvider : CodeVisionProvider<Unit> {

    override fun computeCodeVision(editor: Editor, uiData: Unit): CodeVisionState {
        return runReadAction {
            val project = editor.project ?: return@runReadAction READY_EMPTY
            val document = editor.document
            val file = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return@runReadAction READY_EMPTY

            val virtualFile = file.viewProvider.virtualFile
            if (ProjectFileIndex.getInstance(project).isInLibrarySource(virtualFile)) return@runReadAction READY_EMPTY

            val text = "Gen tests"
            val icon = IconLoader.getIcon("/icons/pluginIcon_dark.svg", this::class.java.classLoader)
            val lenses = ArrayList<Pair<TextRange, CodeVisionEntry>>()
            for (element in SyntaxTraverser.psiTraverser(file).preOrderDfsTraversal()) {
                getFunctionTextRange(element)?.let {
                    val length = editor.document.textLength
                    val adjustedRange = TextRange(min(it.startOffset, length), min(it.endOffset, length))
                    val clickHandler = GenTestsClickHandler(adjustedRange)
                    val entry = ClickableTextCodeVisionEntry(text, id, onClick = clickHandler, icon, text, text, emptyList())
                    entry.showInMorePopup = false
                    lenses.add(adjustedRange to entry)
                }
            }
            return@runReadAction CodeVisionState.Ready(lenses)

        }
    }

    private fun getFunctionTextRange(element: PsiElement): TextRange? {
        return if (element.elementType.toString() in setOf("FUN", "METHOD")) {
            InlayHintsUtils.getTextRangeWithoutLeadingCommentsAndWhitespaces(element)
        } else { null }
    }


    override fun getPlaceholderCollector(editor: Editor, psiFile: PsiFile?): CodeVisionPlaceholderCollector? {
        if (psiFile == null) return null
        return object : BypassBasedPlaceholderCollector {
            override fun collectPlaceholders(element: PsiElement, editor: Editor): List<TextRange> {
                return listOfNotNull(getFunctionTextRange(element))
            }
        }
    }

    override fun isAvailableFor(project: Project): Boolean { return true }
    override fun precomputeOnUiThread(editor: Editor) {}
    override fun preparePreview(editor: Editor, file: PsiFile) {}
    override val name: String get() = NAME
    override val relativeOrderings: List<CodeVisionRelativeOrdering> get() = emptyList()
    override val defaultAnchor: CodeVisionAnchorKind get() = CodeVisionAnchorKind.Default
    override val id: String get() = ID

    companion object {
        internal const val ID: String = "gen.tests.code.vision"
        internal const val NAME: String = "label.gen.tests.inlay.hints"
    }
}

private class GenTestsClickHandler(val textRange: TextRange) : (MouseEvent?, Editor) -> Unit {
    override fun invoke(event: MouseEvent?, editor: Editor) {
        event ?: return
        val component = event.component as? JComponent ?: return
        val selectionModel = editor.selectionModel
        selectionModel.setSelection(textRange.startOffset, textRange.endOffset)
        val action = ActionManager.getInstance().getAction("ai.devchat.idea.action.AddToDevChatEditorAction")
        ActionUtil.invokeAction(action, component, ActionPlaces.EDITOR_INLAY, event, null)
    }
}
