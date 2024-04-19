package ai.devchat.plugin.hints

import ai.devchat.common.DevChatBundle
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.editor.Editor
import com.intellij.psi.*
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import javax.swing.Icon

class DocStringCVProvider : ChatCVProviderBase() {
    override fun buildPayload(editor: Editor, element: PsiElement): JSONObject {
        editor.selectionModel.setSelection(element.startOffset, null, element.endOffset)
        return JSONObject(mapOf("message" to "/docstring"))
    }

    override fun getIcon(): Icon? {
        return null
    }

    override fun getHint(element: PsiElement, file: PsiFile): String {
        return DevChatBundle.message("settings.code.vision.docstring.hint")
    }

    override val name: String get() = NAME
    override val id: String get() = ID
    override val groupId: String get() = GROUP_ID

    companion object {
        internal const val ID: String = "docstring.code.vision"
        internal const val NAME: String = "label.docstring.inlay.hints"
        internal const val GROUP_ID: String = "docstring"
    }
}
