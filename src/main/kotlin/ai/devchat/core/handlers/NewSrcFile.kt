package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.plugin.currentProject
import com.alibaba.fastjson.JSONObject
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager

class NewSrcFile(requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.NEW_SRC_FILE_RESPONSE
    override fun action() {
        val content = payload!!.getString("content")
        val language = payload!!.getString("language")
        ApplicationManager.getApplication().invokeLater {
            val project = currentProject ?: return@invokeLater
            val dir = FileEditorManager.getInstance(project).selectedEditor?.file?.parent ?: return@invokeLater
            WriteCommandAction.runWriteCommandAction(project) {
                val psiDirectory = PsiManager.getInstance(project).findDirectory(dir) ?: return@runWriteCommandAction
                val fileLanguage = getLanguageByName(language) ?: return@runWriteCommandAction
                val psiFile = PsiFileFactory.getInstance(project).createFileFromText(fileLanguage, content)
                val addedFile = (psiDirectory.add(psiFile) as PsiFile).virtualFile
                addedFile.refresh(false, false)
                FileEditorManager.getInstance(project).openFile(addedFile, true)
            }
        }
        send()
    }

    private fun getLanguageByName(markdownName: String): Language? {
        val markdownToLanguageId = mapOf(
            "java" to "JAVA",
            "kotlin" to "kotlin",
            "python" to "Python",
            "js" to "JavaScript",
        )
        val languageId = markdownToLanguageId[markdownName.lowercase()] ?: return null
        return Language.findLanguageByID(languageId)
    }

}
