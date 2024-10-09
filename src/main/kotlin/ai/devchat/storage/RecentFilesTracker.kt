package ai.devchat.storage

import ai.devchat.common.Log
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.util.messages.MessageBusConnection


@Service(Service.Level.PROJECT)
class RecentFilesTracker(private val project: Project) {
    private val maxSize = 10

    private val recentFiles: MutableList<VirtualFile> = mutableListOf()
    private val projectFileIndex = ProjectFileIndex.getInstance(this.project)

    init {
        Log.info("RecentFilesTracker initialized for project: ${project.name}")
        val connection: MessageBusConnection = project.messageBus.connect()
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                if (file.isFile) {
                    addRecentFile(file)
                }
            }
        })

        // Init with open files
        FileEditorManager.getInstance(project).openFiles.forEach { addRecentFile(it) }
    }

    private fun addRecentFile(file: VirtualFile) = runInEdt {
        if (file.isFile && projectFileIndex.isInContent(file)) {
            recentFiles.remove(file)
            recentFiles.add(0, file)
            if (recentFiles.size > maxSize) {
                recentFiles.removeAt(recentFiles.size - 1)
            }
        }
    }

    fun getRecentFiles(): List<VirtualFile> {
        return recentFiles.toList()
    }
}

class RecentFilesStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        project.service<RecentFilesTracker>()
    }
}
