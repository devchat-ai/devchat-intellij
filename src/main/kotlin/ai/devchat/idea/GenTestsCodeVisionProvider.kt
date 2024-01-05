package ai.devchat.idea

import com.intellij.codeInsight.codeVision.*
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.codeInsight.hints.codeVision.DaemonBoundCodeVisionProvider
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.elementType
import java.awt.event.MouseEvent
import java.lang.Integer.min
import javax.swing.JComponent

class GenTestsCodeVisionProvider : DaemonBoundCodeVisionProvider {

    override fun computeForEditor(editor: Editor, file: PsiFile): List<Pair<TextRange, CodeVisionEntry>> {
        val project = editor.project ?: return emptyList()
        val virtualFile = file.viewProvider.virtualFile
        if (ProjectFileIndex.getInstance(project).isInLibrarySource(virtualFile)) return emptyList()

        val text = "Gen tests"
        val icon = IconLoader.getIcon("/icons/pluginIcon_dark.svg", this::class.java.classLoader)
        val lenses = ArrayList<Pair<TextRange, CodeVisionEntry>>()
        for (element in SyntaxTraverser.psiTraverser(file).preOrderDfsTraversal()) {
            if (element.elementType.toString() !in setOf("FUN", "METHOD")) continue
            val length = editor.document.textLength
            val textRange = TextRange(
                min(element.textRange.startOffset, length),
                min(element.textRange.endOffset, length)
            )
            val clickHandler = GenTestsClickHandler(textRange)
            val entry = ClickableTextCodeVisionEntry(text, id, onClick = clickHandler, icon, text, text, emptyList())
            entry.showInMorePopup = false
            lenses.add(textRange to entry)
        }
        return lenses
    }
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
