package ai.devchat.plugin.hints

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
        return "Add docstring"
    }

    override val name: String get() = NAME
    override val id: String get() = ID

    companion object {
        internal const val ID: String = "docstring.code.vision"
        internal const val NAME: String = "label.docstring.inlay.hints"
    }
}
