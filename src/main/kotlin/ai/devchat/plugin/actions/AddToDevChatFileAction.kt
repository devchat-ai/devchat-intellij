package ai.devchat.plugin.actions

import ai.devchat.common.Constants.ASSISTANT_NAME_ZH
import ai.devchat.common.DevChatBundle
import ai.devchat.plugin.DevChatService
import ai.devchat.storage.CONFIG
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.wm.ToolWindowManager
import java.io.IOException
import java.nio.charset.StandardCharsets

class AddToDevChatFileAction : AnAction() {
    private var addToDevChatAction: AddToDevChatAction? = null

    override fun update(e: AnActionEvent) {
        val context = e.dataContext
        val virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(context)
        val enabled = virtualFile != null && !virtualFile.isDirectory && virtualFile.exists()
        e.presentation.isEnabled = enabled
        if ((CONFIG["language"] as? String) == "zh") {
            e.presentation.text = DevChatBundle.message("action.addToDevChat.text.zh")
        }
        addToDevChatAction = AddToDevChatAction(e.project!!)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val context = e.dataContext
        val virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(context)
        val project = e.project
        val projectPath = project!!.basePath
        val absoluteFilePath = virtualFile!!.path
        var relativePath = absoluteFilePath
        if (projectPath != null && absoluteFilePath.startsWith(projectPath)) {
            relativePath = absoluteFilePath.substring(projectPath.length + 1)
        }
        val fileType = virtualFile.fileType
        val language = fileType.name
        if (!virtualFile.isDirectory) {
            try {
                ToolWindowManager.getInstance(project).getToolWindow(ASSISTANT_NAME_ZH)?.show {
                    val bytes = virtualFile.contentsToByteArray()
                    val content = String(bytes, StandardCharsets.UTF_8)
                    val uiLoaded = project.getService(DevChatService::class.java).uiLoaded
                    addToDevChatAction!!.execute(relativePath, content, language, 0, !uiLoaded)
                }
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        } else {
            throw RuntimeException("invalid virtualFile.")
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
