package ai.devchat.plugin.completion.editor

import ai.devchat.storage.CONFIG
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.*
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicLong

class Debouncer(private val debounceDelay: Long, private val scope: CoroutineScope) {
    private val lastTimestamp = AtomicLong(0)

    fun debounce(action: suspend () -> Unit): Job = scope.launch {
        val timestamp = System.currentTimeMillis()
        lastTimestamp.set(timestamp)
        delay(debounceDelay)
        if (timestamp == lastTimestamp.get()) {
            action()
        }
    }
}

class EditorListener : EditorFactoryListener {
    private val logger = Logger.getInstance(EditorListener::class.java)
    private val disposers = mutableMapOf<Editor, () -> Unit>()
    private val debouncer = Debouncer(300, CoroutineScope(Dispatchers.Default))

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        val editorManager = editor.project?.let { FileEditorManager.getInstance(it) } ?: return
        val completionProvider = service<CompletionProvider>()
        val inlineCompletionService = service<InlineCompletionService>()
        logger.debug("EditorFactoryListener: editorCreated $event")

        editor.caretModel.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                logger.debug("CaretListener: caretPositionChanged $event")
                if (editorManager.selectedTextEditor == editor) {
                    inlineCompletionService.shownInlineCompletion?.let {
                        if (it.ongoing) return
                    }
                    completionProvider.ongoingCompletion.value.let {
                        if (it != null && it.editor == editor && it.offset == editor.caretModel.primaryCaret.offset) {
                            logger.debug("Keep ongoing completion.")
                        } else {
                            completionProvider.clear()
                        }
                    }
                }
            }
        })

        val documentListener = object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                logger.info("DocumentListener: documentChanged $event")

                debouncer.debounce {
                    ApplicationManager.getApplication().invokeLater({
                        processDocumentChange(event, editor, editorManager, completionProvider, inlineCompletionService)
                    }, ModalityState.defaultModalityState())
                }
            }
        }
        editor.document.addDocumentListener(documentListener)

        val messagesConnection = editor.project?.messageBus?.connect()
        messagesConnection?.subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    logger.debug("FileEditorManagerListener: selectionChanged.")
                    completionProvider.clear()
                }
            }
        )

        disposers[editor] = {
            editor.document.removeDocumentListener(documentListener)
            messagesConnection?.disconnect()
        }
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        logger.debug("EditorFactoryListener: editorReleased $event")
        disposers[event.editor]?.invoke()
        disposers.remove(event.editor)
    }

    private fun processDocumentChange(
        event: DocumentEvent,
        editor: Editor,
        editorManager: FileEditorManager,
        completionProvider: CompletionProvider,
        inlineCompletionService: InlineCompletionService
    ) {
        logger.info("trigger processDocumentChange")
        if (editorManager.selectedTextEditor == editor) {
            val enabled = CONFIG["complete_enable"] as? Boolean ?: false
            if (enabled) {
                inlineCompletionService.shownInlineCompletion?.let {
                    if (it.ongoing) {
                        logger.info("Ongoing inline completion, skipping.")
                        return
                    }
                }

                completionProvider.ongoingCompletion.value?.let {
                    if (it.editor == editor && it.offset == editor.caretModel.primaryCaret.offset) {
                        logger.info("Keeping ongoing completion.")
                    } else {
                        logger.info("Cancelling previous completion and providing new one.")
                        completionProvider.clear()
                        completionProvider.provideCompletion(editor, editor.caretModel.primaryCaret.offset)
                    }
                } ?: run {
                    logger.info("Providing new completion.")
                    completionProvider.provideCompletion(editor, editor.caretModel.primaryCaret.offset)
                }
            } else {
                logger.debug("Completion is disabled.")
            }
        } else {
            logger.debug("Not the selected editor.")
        }
    }
}