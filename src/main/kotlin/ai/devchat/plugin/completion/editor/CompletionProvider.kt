package ai.devchat.plugin.completion.editor

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import ai.devchat.plugin.completion.agent.AgentService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@Service
class CompletionProvider {
    private val logger = Logger.getInstance(CompletionProvider::class.java)
    private var completionSequence = AtomicInteger(0)

    data class CompletionContext(val editor: Editor, val offset: Int, val job: Job)

    private val ongoingCompletionFlow: MutableStateFlow<CompletionContext?> = MutableStateFlow(null)
    val ongoingCompletion = ongoingCompletionFlow.asStateFlow()

    private val currentContext = AtomicReference<CompletionContext?>(null)

    fun provideCompletion(editor: Editor, offset: Int, manually: Boolean = false) {
        val currentSequence = completionSequence.incrementAndGet()
        val agentService = service<AgentService>()
        val inlineCompletionService = service<InlineCompletionService>()

        val oldContext = currentContext.getAndSet(null)
        oldContext?.job?.cancel()
        inlineCompletionService.dismiss()

        val job = agentService.scope.launch {
            logger.info("Trigger completion at $offset")
            agentService.provideCompletion(editor, offset, manually).let {
                if (isActive) {  // Check if the job is still active before updating
                    if (it != null && completionSequence.get() == currentSequence) {
                        logger.info("Show completion at $offset: $it")
                        inlineCompletionService.show(editor, offset, it)
                    }
                    currentContext.set(null)
                    ongoingCompletionFlow.value = null
                }
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
}