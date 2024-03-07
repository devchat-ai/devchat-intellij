package ai.devchat.plugin.hints

import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.*
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset

class UnitTestsCVProvider : ChatCVProviderBase() {
    override fun buildPayload(editor: Editor, element: PsiElement): JSONObject {
        return JSONObject(
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
    }

    override fun getHint(element: PsiElement, file: PsiFile): String {
        return "Add unit tests"
    }

    override val name: String get() = NAME
    override val id: String get() = ID

    companion object {
        internal const val ID: String = "gen.tests.code.vision"
        internal const val NAME: String = "label.gen.tests.inlay.hints"
    }
}
