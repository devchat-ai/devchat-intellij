package ai.devchat.idea.action

import ai.devchat.idea.DevChatToolWindow
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.wm.ToolWindowManager
import java.io.IOException
import java.nio.charset.StandardCharsets

class AddToDevChatFileAction : AnAction() {
    private val addToDevChatAction: AddToDevChatAction = AddToDevChatAction()

    override fun update(e: AnActionEvent) {
        val context = e.dataContext
        val virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(context)
        val enabled = virtualFile != null && !virtualFile.isDirectory && virtualFile.exists()
        e.presentation.isEnabled = enabled
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
                ToolWindowManager.getInstance(project).getToolWindow("DevChat")?.show {
                    val bytes = virtualFile.contentsToByteArray()
                    val content = String(bytes, StandardCharsets.UTF_8)
                    addToDevChatAction.execute(relativePath, content, language, 0, !DevChatToolWindow.loaded)
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
