package ai.devchat.plugin.completion.agent

import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import io.ktor.util.*
import kotlinx.coroutines.*

@Service
class AgentService : Disposable {
  val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
  private var agent: Agent = Agent(
    scope,
    endpoint = System.getenv("NVAPI_ENDPOINT"),
    apiKey = System.getenv("NVAPI_KEY"),
  )

  suspend fun provideCompletion(editor: Editor, offset: Int, manually: Boolean = false): Agent.CompletionResponse? {
    return ReadAction.compute<PsiFile, Throwable> {
      editor.project?.let { project ->
        PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
      }
    }?.let { file ->
      agent.provideCompletions(
        Agent.CompletionRequest(
          file.virtualFile.path,
          file.getLanguageId(),
          editor.document.text,
          offset,
          manually,
        )
      )
    }
  }

  suspend fun postEvent(event: Agent.LogEventRequest) {
    agent.postEvent(event)
  }

  override fun dispose() {
  }

  companion object {
    private fun PsiFile.getLanguageId(): String {
      return if (this.language != Language.ANY &&
        this.language.id.isNotBlank() &&
        this.language.id.toLowerCasePreservingASCIIRules() !in arrayOf("txt", "text", "textmate")
      ) {
        languageIdMap[this.language.id] ?: this.language.id
          .toLowerCasePreservingASCIIRules()
          .replace("#", "sharp")
          .replace("++", "pp")
          .replace(" ", "")
      } else {
        val ext = this.fileType.defaultExtension.ifBlank {
          this.virtualFile.name.substringAfterLast(".")
        }
        if (ext.isNotBlank()) {
          filetypeMap[ext] ?: ext.toLowerCasePreservingASCIIRules()
        } else {
          "plaintext"
        }
      }
    }

    private val languageIdMap = mapOf(
      "ObjectiveC" to "objective-c",
      "ObjectiveC++" to "objective-cpp",
    )
    private val filetypeMap = mapOf(
      "py" to "python",
      "js" to "javascript",
      "cjs" to "javascript",
      "mjs" to "javascript",
      "jsx" to "javascriptreact",
      "ts" to "typescript",
      "tsx" to "typescriptreact",
      "kt" to "kotlin",
      "md" to "markdown",
      "cc" to "cpp",
      "cs" to "csharp",
      "m" to "objective-c",
      "mm" to "objective-cpp",
      "sh" to "shellscript",
      "zsh" to "shellscript",
      "bash" to "shellscript",
      "txt" to "plaintext",
    )
  }
}