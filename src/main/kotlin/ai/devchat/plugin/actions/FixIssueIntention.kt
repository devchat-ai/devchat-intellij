package ai.devchat.plugin.actions

import ai.devchat.common.Constants.ASSISTANT_NAME_EN
import ai.devchat.common.Constants.ASSISTANT_NAME_ZH
import ai.devchat.core.DevChatActions
import ai.devchat.core.handlers.SendUserMessageHandler
import ai.devchat.plugin.DevChatService
import com.alibaba.fastjson.JSONObject
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiFile

class FixIssueIntention : IntentionAction, PriorityAction {
    override fun getText(): String = "Fix using $ASSISTANT_NAME_EN"
    override fun getFamilyName(): String = "DevChat"
    override fun getPriority(): PriorityAction.Priority = PriorityAction.Priority.HIGH
    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return true
    }
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        editor?.let{
            val line = it.document.getLineNumber(it.caretModel.offset)
            val lineStartOffset: Int = it.document.getLineStartOffset(line)
            val lineEndOffset: Int = it.document.getLineEndOffset(line)
            it.selectionModel.setSelection(lineStartOffset, lineEndOffset)
            val payload = JSONObject(mapOf("message" to "/fix_issue"))

            ToolWindowManager.getInstance(editor.project!!).getToolWindow(ASSISTANT_NAME_ZH)?.show {
                val uiLoaded = editor.project!!.getService(DevChatService::class.java).uiLoaded
                if (uiLoaded) {
                    SendUserMessageHandler(project, DevChatActions.SEND_USER_MESSAGE_REQUEST,null, payload).executeAction()
                } else {
                    SendUserMessageHandler.cache = payload
                }
            }
        }
    }
    override fun startInWriteAction(): Boolean = false
}
