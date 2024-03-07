package ai.devchat.plugin.hints

import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.editor.Editor
import com.intellij.psi.*
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import javax.swing.Icon

class ExplainCodeCVProvider : ChatCVProviderBase() {
    override fun buildPayload(editor: Editor, element: PsiElement): JSONObject {
        editor.selectionModel.setSelection(element.startOffset, null, element.endOffset)
        return JSONObject(mapOf("message" to "/explain"))
    }

    override fun getIcon(): Icon? {
        return null
    }

    override fun getHint(element: PsiElement, file: PsiFile): String {
        return "Explain"
    }

    override val name: String get() = NAME
    override val id: String get() = ID

    companion object {
        internal const val ID: String = "explain.code.vision"
        internal const val NAME: String = "label.explain.inlay.hints"
    }
}
