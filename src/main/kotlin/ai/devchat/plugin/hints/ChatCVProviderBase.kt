package ai.devchat.plugin.hints

import ai.devchat.common.Constants.ASSISTANT_NAME_ZH
import ai.devchat.common.Constants.FUNC_TYPE_NAMES
import ai.devchat.core.DevChatActions
import ai.devchat.core.handlers.SendUserMessageHandler
import ai.devchat.plugin.DevChatService
import com.alibaba.fastjson.JSONObject
import com.intellij.codeInsight.codeVision.CodeVisionAnchorKind
import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.codeInsight.hints.codeVision.CodeVisionProviderBase
import com.intellij.codeInsight.hints.settings.language.isInlaySettingsEditor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SyntaxTraverser
import com.intellij.psi.util.elementType
import java.awt.event.MouseEvent
import javax.swing.Icon

abstract class ChatCVProviderBase : CodeVisionProviderBase() {

    abstract fun buildPayload(editor: Editor, element: PsiElement): JSONObject
    open fun getIcon(): Icon? {
        return IconLoader.getIcon("/icons/toolWindowIcon.svg", this::class.java.classLoader)
    }

    override fun computeForEditor(editor: Editor, file: PsiFile): List<Pair<TextRange, CodeVisionEntry>> {
        if (file.project.isDefault) return emptyList()
        if (!acceptsFile(file)) return emptyList()

        val virtualFile = file.viewProvider.virtualFile
        if (ProjectFileIndex.getInstance(file.project).isInLibrarySource(virtualFile)) return emptyList()

        val lenses = ArrayList<Pair<TextRange, CodeVisionEntry>>()
        val icon = getIcon()
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
        val project = editor.project!!

        ToolWindowManager.getInstance(project).getToolWindow(ASSISTANT_NAME_ZH)?.show {
            val uiLoaded = project.getService(DevChatService::class.java).uiLoaded
            if (uiLoaded) {
                SendUserMessageHandler(project, DevChatActions.SEND_USER_MESSAGE_REQUEST,null, payload).executeAction()
            } else {
                SendUserMessageHandler.cache = payload
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
}