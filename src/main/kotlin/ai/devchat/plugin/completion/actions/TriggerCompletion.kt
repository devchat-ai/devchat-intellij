package ai.devchat.plugin.completion.actions

import ai.devchat.common.DevChatBundle
import ai.devchat.plugin.completion.editor.CompletionProvider
import ai.devchat.storage.CONFIG
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service


class TriggerCompletion : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val completionScheduler = service<CompletionProvider>()
    val editor = e.getData(CommonDataKeys.EDITOR) ?: return
    val offset = editor.caretModel.primaryCaret.offset
    completionScheduler.provideCompletion(editor, offset, manually = true)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = e.project != null
        && e.getData(CommonDataKeys.EDITOR) != null
    if ((CONFIG["language"] as? String) == "zh") {
      e.presentation.text = DevChatBundle.message("action.triggerCompletion.text.zh")
    } else {
      e.presentation.text = DevChatBundle.message("action.triggerCompletion.text")
    }
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }
}
