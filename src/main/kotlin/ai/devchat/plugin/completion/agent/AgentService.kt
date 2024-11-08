package ai.devchat.plugin.completion.agent

import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException

@Service
class AgentService : Disposable {
  val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
  private var agent: Agent = Agent(scope)

suspend fun provideCompletion(editor: Editor, offset: Int, manually: Boolean = false): Agent.CompletionResponse? {
    println("Entering provideCompletion method")
    return withContext(Dispatchers.Default) {
        try {
            println("Attempting to get PsiFile")
            val file = suspendCancellableCoroutine<PsiFile?> { continuation ->
                ApplicationManager.getApplication().invokeLater({
                    val psiFile = ReadAction.compute<PsiFile?, Throwable> {
                        editor.project?.let { project ->
                            PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
                        }
                    }
                    continuation.resume(psiFile)
                }, ModalityState.defaultModalityState())
            }

            println("PsiFile obtained: ${file != null}")

            file?.let { psiFile ->
                println("Calling agent.provideCompletions")
                val result = agent.provideCompletions(
                    Agent.CompletionRequest(
                        psiFile,
                        psiFile.getLanguageId(),
                        offset,
                        manually,
                    )
                )
                println("agent.provideCompletions returned: $result")
                result
            }
        } catch (e: CancellationException) {
            // 方案1：以较低的日志级别记录
            println("Completion was cancelled: ${e.message}")
            // 或者方案2：完全忽略
            // // 不做任何处理

            null
        } catch (e: Exception) {
            println("Exception in provideCompletion: ${e.message}")
            e.printStackTrace()
            null
        }
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