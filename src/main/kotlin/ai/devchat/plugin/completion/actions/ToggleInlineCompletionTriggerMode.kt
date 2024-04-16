package ai.devchat.plugin.completion.actions

import ai.devchat.storage.CompletionTriggerMode
import ai.devchat.storage.DevChatState
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

class ToggleInlineCompletionTriggerMode : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val state = service<DevChatState>()
    state.completionTriggerMode = when (state.completionTriggerMode) {
      CompletionTriggerMode.AUTOMATIC -> CompletionTriggerMode.MANUAL
      CompletionTriggerMode.MANUAL -> CompletionTriggerMode.AUTOMATIC
    }
  }

  override fun update(e: AnActionEvent) {
    val state = service<DevChatState>()
    if (state.completionTriggerMode == CompletionTriggerMode.AUTOMATIC) {
      e.presentation.text = "Switch to Manual Mode"
      e.presentation.description = "Manual trigger inline completion suggestions on demand."
    } else {
      e.presentation.text = "Switch to Automatic Mode"
      e.presentation.description = "Show inline completion suggestions automatically."
    }
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }
}