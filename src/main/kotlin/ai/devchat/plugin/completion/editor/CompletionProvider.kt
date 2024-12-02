package ai.devchat.plugin.completion.editor

import ai.devchat.plugin.Browser
import ai.devchat.plugin.DevChatService
import ai.devchat.plugin.completion.agent.Agent.LogEventRequest
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import ai.devchat.plugin.completion.agent.AgentService
import ai.devchat.plugin.completion.agent.Agent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager


@Service
class CompletionProvider {
    private val logger = Logger.getInstance(CompletionProvider::class.java)
    private var completionSequence = AtomicInteger(0)

    data class CompletionContext(val editor: Editor, val offset: Int, val job: Job)

    private val ongoingCompletionFlow: MutableStateFlow<CompletionContext?> = MutableStateFlow(null)
    val ongoingCompletion = ongoingCompletionFlow.asStateFlow()

    private val currentContext = AtomicReference<CompletionContext?>(null)

    fun provideCompletion(editor: Editor, offset: Int, manually: Boolean = false) {
        logger.info("start provideCompletion")
        val currentSequence = completionSequence.incrementAndGet()
        val agentService = service<AgentService>()
        val inlineCompletionService = service<InlineCompletionService>()

        val oldContext = currentContext.getAndSet(null)
        oldContext?.job?.cancel()
        inlineCompletionService.dismiss()

        val job = agentService.scope.launch {
            try {
                logger.info("Trigger completion at $offset")
                val result = agentService.provideCompletion(editor, offset, manually)
                if (isActive && result != null && completionSequence.get() == currentSequence) {
                    logger.info("Show completion at $offset: $result")
                    logCompletionMessage(editor, result)
                    ApplicationManager.getApplication().invokeLater {
                        inlineCompletionService.show(editor, offset, result, manually)
                    }
                }
            } catch (e: CancellationException) {
                // 方案1：以较低的日志级别记录
                logger.info("Completion was cancelled: ${e.message}")
                // 或者方案2：完全忽略
                // // 不做任何处理

                null
            } catch (e: Exception) {
                logger.error("Error in completion job", e)
            } finally {
                currentContext.set(null)
                ongoingCompletionFlow.value = null
            }
        }

        val newContext = CompletionContext(editor, offset, job)
        currentContext.set(newContext)
        ongoingCompletionFlow.value = newContext
    }

    fun clear() {
        val inlineCompletionService = service<InlineCompletionService>()
        inlineCompletionService.dismiss()
        val context = currentContext.getAndSet(null)
        context?.job?.cancel()
        ongoingCompletionFlow.value = null
    }

    fun logCompletionMessage(editor: Editor, completion: Agent.CompletionResponse,) {
        var browser = editor.project?.getService(DevChatService::class.java)?.browser
        val virtualFile = FileDocumentManager.getInstance().getFile(editor.document)
        val choice = completion.choices.first()
        val text = choice.text
        if (text.isBlank()) {
            return
        }

        val message = mapOf(
            "command" to "logMessage",
            "id" to completion.id,
            "language" to (virtualFile?.extension ?: ""),
            "commandName" to "code_completion",
            "content" to text,
            "model" to completion.model
        )

        // 使用 Browser 类的 sendToWebView 方法发送消息
        if (browser == null) {
            logger.warn("Browser instance is null, cannot send log event to webview.")
        } else {
            browser.sendToWebView(message)
        }

        // 记录日志
        logger.info("Code completion log message: $message")
    }
}