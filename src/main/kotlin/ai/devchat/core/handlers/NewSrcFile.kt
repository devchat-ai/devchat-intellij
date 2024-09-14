package ai.devchat.core.handlers

import ai.devchat.core.BaseActionHandler
import ai.devchat.core.DevChatActions
import ai.devchat.plugin.currentProject
import com.alibaba.fastjson.JSONObject
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager

class NewSrcFile(project: Project, requestAction: String, metadata: JSONObject?, payload: JSONObject?) : BaseActionHandler(
    project,
    requestAction,
    metadata,
    payload
) {
    override val actionName: String = DevChatActions.NEW_SRC_FILE_RESPONSE
    override fun action() {
        val content = payload!!.getString("content")
        val language = payload!!.getString("language")
        runInEdt {
            val project = currentProject ?: return@runInEdt
            val dir = FileEditorManager.getInstance(project).selectedEditor?.file?.parent ?: return@runInEdt
            ApplicationManager.getApplication().runWriteAction {
                val psiDirectory = PsiManager.getInstance(project).findDirectory(dir) ?: return@runWriteAction
                val (fileLanguage, ext) = getLanguageByName(language) ?: return@runWriteAction
                val psiFile = PsiFileFactory.getInstance(project).createFileFromText(
                    "foo$ext", fileLanguage, content
                )
                val addedFile = (psiDirectory.add(psiFile) as PsiFile).virtualFile
                addedFile.refresh(false, false)
                FileEditorManager.getInstance(project).openFile(addedFile, true)
            }
        }
        send()
    }

    private fun getLanguageByName(markdownName: String): Pair<Language, String>? {
        val markdownToLanguageId = mapOf(
            // IDEA
            "toml" to Pair("TOML", ".toml"),
            "html" to Pair("HTML", ".html"),
            "json" to Pair("JSON", ".json"),
            "xml" to Pair("XML", ".xml"),
            "yaml" to Pair("yaml", ".yaml"),
            "yml" to Pair("yaml", ".yml"),
            "markdown" to Pair("Markdown", ".md"),
            "shell" to Pair("Shell Script", ".sh"),
            "sh" to Pair("Shell Script", ".sh"),
            "java" to Pair("JAVA", ".java"),
            "groovy" to Pair("Groovy", ".groovy"),
            "kotlin" to Pair("kotlin", ".kt"),

            // Webstorm
            "js" to Pair("JavaScript", ".js"),
            "javascript" to Pair("JavaScript", ".js"),
            "css" to Pair("CSS", ".css"),
            "less" to Pair("LESS", ".less"),
            "vue" to Pair("Vue", ".vue"),
            "ts" to Pair("TypeScript", ".ts"),
            "typescript" to Pair("TypeScript", ".ts"),
            "dockerfile" to Pair("Dockerfile", ".dockerfile"),

            // GoLand
            "makefile" to Pair("Makefile", ""),
            "golang" to Pair("go", ".go"),
            "go" to Pair("go", ".go"),

            // PyCharm
            "python" to Pair("Python", ".py"),

            // RubyMine
            "ruby" to Pair("ruby", ".rb"),
            "rb" to Pair("ruby", ".rb"),
            "tsx" to Pair("TypeScript JSX", ".tsx"),
            "sass" to Pair("SASS", ".sass"),
            "sql" to Pair("SQL", ".sql"),
            "scss" to Pair("SCSS", ".scss"),
            "coffeescript" to Pair("CoffeeScript", ".coffee"),

            // PHPStorm
            "php" to Pair("PHP", ".php"),

            // RustRover
            "rs" to Pair("Rust", ".rs"),
            "rust" to Pair("Rust", ".rs"),

            // CLion
            "cpp" to Pair("CPP", ".cpp"),
            "c" to Pair("C", ".c"),
            "objectivec" to Pair("ObjectiveC", ".m"),

            // Others
            "scala" to Pair("Scala", ".sc"),
        )
        val (languageId, ext) = markdownToLanguageId[markdownName.lowercase()] ?: return null
        return Language.findLanguageByID(languageId)?.let {
            Pair(it, ext)
        }
    }

}
