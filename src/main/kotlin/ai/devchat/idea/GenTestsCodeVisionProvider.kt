package ai.devchat.idea

import ai.devchat.devchat.handler.SendUserMessageHandler
import com.alibaba.fastjson.JSONObject
import com.intellij.codeInsight.codeVision.*
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.codeInsight.hints.codeVision.CodeVisionProviderBase
import com.intellij.codeInsight.hints.settings.language.isInlaySettingsEditor
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.*
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parentOfTypes
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import java.awt.event.MouseEvent
import java.lang.Integer.min
import javax.swing.JComponent

class GenTestsCodeVisionProvider : CodeVisionProviderBase() {

    override fun computeForEditor(editor: Editor, file: PsiFile): List<Pair<TextRange, CodeVisionEntry>> {
        if (file.project.isDefault) return emptyList()
        if (!acceptsFile(file)) return emptyList()

        val virtualFile = file.viewProvider.virtualFile
        if (ProjectFileIndex.getInstance(file.project).isInLibrarySource(virtualFile)) return emptyList()

        val lenses = ArrayList<Pair<TextRange, CodeVisionEntry>>()
        val icon = IconLoader.getIcon("/icons/pluginIcon_dark.svg", this::class.java.classLoader)
        for (element in SyntaxTraverser.psiTraverser(file)) {
            if (!acceptsElement(element)) continue
            val hint = getHint(element, file)
            val handler = ClickHandler(element)
            lenses.add(element.textRange to ClickableTextCodeVisionEntry(hint, id, handler, icon))
        }
        return lenses
    }

    override fun getHint(element: PsiElement, file: PsiFile): String {
        return "Gen tests"
    }

    override fun handleClick(editor: Editor, element: PsiElement, event: MouseEvent?) {
        event ?: return
        val payload = JSONObject(
            mapOf(
                "command" to "genUnitTests",
                "message" to "/unit_tests " + listOf(
                    FileDocumentManager.getInstance().getFile(editor.document)!!.path,
                    (element as? PsiNamedElement)?.name,
                    editor.document.getLineNumber(element.startOffset),
                    editor.document.getLineNumber(element.endOffset),
                    editor.document.getLineNumber(element.parent.startOffset),
                    editor.document.getLineNumber(element.parent.endOffset),
                ).joinToString(":::"),
            )
        )

        if (DevChatToolWindow.loaded) {
            SendUserMessageHandler(null, payload).executeAction()
        } else {
            cache = payload
            ToolWindowManager.getInstance(editor.project!!).getToolWindow("DevChat")?.show()
        }
    }

    override val name: String get() = NAME
    override val relativeOrderings: List<CodeVisionRelativeOrdering> get() = emptyList()
    override fun acceptsElement(element: PsiElement): Boolean {
        return element.elementType.toString() in setOf("FUN", "METHOD")
    }

    override fun acceptsFile(file: PsiFile): Boolean {
        return true
    }

    override val defaultAnchor: CodeVisionAnchorKind get() = CodeVisionAnchorKind.Default
    override val id: String get() = ID
    override val groupId: String get() = super.groupId

    private inner class ClickHandler(element: PsiElement) : (MouseEvent?, Editor) -> Unit {
        private val elementPointer = SmartPointerManager.createPointer(element)

        override fun invoke(event: MouseEvent?, editor: Editor) {
            if (isInlaySettingsEditor(editor)) return
            val element = elementPointer.element ?: return
            handleClick(editor, element, event)
        }
    }

    companion object {
        internal const val ID: String = "gen.tests.code.vision"
        internal const val NAME: String = "label.gen.tests.inlay.hints"
        var cache: JSONObject? = null
    }
}

