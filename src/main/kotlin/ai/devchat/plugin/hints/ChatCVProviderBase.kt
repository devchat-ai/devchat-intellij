package ai.devchat.plugin.hints

import ai.devchat.core.DevChatActions
import ai.devchat.core.handlers.SendUserMessageHandler
import ai.devchat.plugin.DevChatToolWindow
import com.alibaba.fastjson.JSONObject
import com.intellij.codeInsight.codeVision.*
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.codeInsight.hints.codeVision.CodeVisionProviderBase
import com.intellij.codeInsight.hints.settings.language.isInlaySettingsEditor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.*
import com.intellij.psi.util.elementType
import java.awt.event.MouseEvent

abstract class ChatCVProviderBase : CodeVisionProviderBase() {

    abstract fun buildPayload(editor: Editor, element: PsiElement): JSONObject

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
            lenses.add(element.textRange to ClickableTextCodeVisionEntry(hint!!, id, handler, icon))
        }
        return lenses
    }

    override fun handleClick(editor: Editor, element: PsiElement, event: MouseEvent?) {
        event ?: return
        val payload = buildPayload(editor, element)

        ToolWindowManager.getInstance(editor.project!!).getToolWindow("DevChat")?.show {
            if (DevChatToolWindow.loaded) {
                SendUserMessageHandler(DevChatActions.SEND_USER_MESSAGE_REQUEST,null, payload).executeAction()
            } else {
                cache = payload
            }
        }
    }

    override val relativeOrderings: List<CodeVisionRelativeOrdering> get() = emptyList()
    override fun acceptsElement(element: PsiElement): Boolean {
        return element.elementType.toString() in FUNC_TYPE_NAMES
    }

    override fun acceptsFile(file: PsiFile): Boolean {
        return true
    }

    override val defaultAnchor: CodeVisionAnchorKind get() = CodeVisionAnchorKind.Default
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
        var cache: JSONObject? = null
    }
}

internal val FUNC_TYPE_NAMES: Set<String> = setOf(
    "FUN", // Kotlin
    "METHOD", // Java
    "FUNCTION_DEFINITION", // C, C++
    "Py:FUNCTION_DECLARATION", // Python
    "FUNCTION_DECLARATION", "METHOD_DECLARATION", // Golang
    "JS:FUNCTION_DECLARATION", "JS:FUNCTION_EXPRESSION", // JS
    "JS:TYPESCRIPT_FUNCTION", "JS:TYPESCRIPT_FUNCTION_EXPRESSION",  // TS
    "CLASS_METHOD", // PHP
    "FUNCTION", // PHP, Rust
    "Ruby:METHOD", // Ruby
)