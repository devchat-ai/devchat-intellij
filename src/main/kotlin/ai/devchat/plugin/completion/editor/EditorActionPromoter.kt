package ai.devchat.plugin.completion.editor

import com.intellij.openapi.actionSystem.ActionPromoter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.Logger
import ai.devchat.plugin.completion.actions.HasPriority

class EditorActionPromoter: ActionPromoter {
  private val logger = Logger.getInstance(EditorActionPromoter::class.java)
  override fun promote(actions: List<AnAction>, context: DataContext): List<AnAction> {
    logger.debug("Promote actions: $actions")
    return actions.sortedByDescending {
      if (it is HasPriority) {
        it.priority
      } else {
        0
      }
    }
  }
}