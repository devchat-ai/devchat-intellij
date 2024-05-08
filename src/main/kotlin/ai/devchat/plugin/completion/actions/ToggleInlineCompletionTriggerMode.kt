package ai.devchat.plugin.completion.actions

import ai.devchat.storage.CONFIG
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ToggleInlineCompletionTriggerMode : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val enabled = CONFIG["complete_enable"] as? Boolean ?: false
    CONFIG["complete_enable"] = !enabled
  }

  override fun update(e: AnActionEvent) {
    val enabled = CONFIG["complete_enable"] as? Boolean ?: false
    if (enabled) {
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