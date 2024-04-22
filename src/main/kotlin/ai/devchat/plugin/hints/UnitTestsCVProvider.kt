package ai.devchat.plugin.hints

import ai.devchat.common.DevChatBundle
import ai.devchat.storage.CONFIG
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
        return if ((CONFIG["language"] as? String) == "zh") {
            DevChatBundle.message("settings.code.vision.unitTests.hint.zh")
        } else {
            DevChatBundle.message("settings.code.vision.unitTests.hint")
        }
    }


    override val name: String get() = NAME
    override val id: String get() = ID
    override val groupId: String get() = GROUP_ID

    companion object {
        internal const val ID: String = "unittests.code.vision"
        internal const val NAME: String = "label.unittests.inlay.hints"
        internal const val GROUP_ID: String = "unitTests"
    }
}
