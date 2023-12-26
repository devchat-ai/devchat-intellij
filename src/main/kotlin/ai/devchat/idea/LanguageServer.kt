package ai.devchat.idea

import ai.devchat.idea.balloon.DevChatNotifier
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.searches.DefinitionsScopedSearch
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File
import java.net.ServerSocket


const val START_PORT: Int = 31800

fun findAvailablePort(startPort: Int): Int {
    var port = startPort
    while (true) {
        try {
            ServerSocket(port).use { return port }
        } catch (ex: Exception) {
            port++
        }
    }
}

fun Project.getSymbol(filePath: String, lineNumber: Int, columnIndex: Int): PsiElement? {
    val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(File(filePath))
    val psiFile = PsiManager.getInstance(this).findFile(virtualFile!!)
    val document = PsiDocumentManager.getInstance(this).getDocument(psiFile!!)
    val offset = document!!.getLineStartOffset(lineNumber - 1) + columnIndex - 1

    return psiFile.findElementAt(offset)
}

class LanguageServer(private var project: Project) {
    private var server: ApplicationEngine? = null

    fun start() {
        val port = findAvailablePort(START_PORT)
        server = embeddedServer(Netty, port=port) {
            routing {
                get("/references") {
                    val filePath = call.parameters["abspath"]
                    val lineNumber = call.parameters["line"]?.toIntOrNull()
                    val columnIndex = call.parameters["character"]?.toIntOrNull()
                    if (filePath == null || lineNumber == null || columnIndex == null) {
                        call.respond(HttpStatusCode.BadRequest, "Missing parameters")
                        return@get
                    }

                    val element = project.getSymbol(filePath, lineNumber, columnIndex)

                    call.respond(element?.references?.toList().orEmpty())
                }

                get("/definitions") {
                    val filePath = call.parameters["abspath"]
                    val lineNumber = call.parameters["line"]?.toIntOrNull()
                    val columnIndex = call.parameters["character"]?.toIntOrNull()
                    if (filePath == null || lineNumber == null || columnIndex == null) {
                        call.respond(HttpStatusCode.BadRequest, "Missing parameters")
                        return@get
                    }

                    val element = project.getSymbol(filePath, lineNumber, columnIndex)
                    val definitions = DefinitionsScopedSearch.search(element).findAll()

                    call.respond(definitions.toList())
                }
            }
        }

        // Register listener to stop the server when project closed
        ProjectManager.getInstance().addProjectManagerListener(
            project, object: ProjectManagerListener {
                override fun projectClosed(project: Project) {
                    super.projectClosed(project)
                    server?.stop(1_000, 2_000)
                }
            }
        )

        server?.start(wait = false)
        DevChatNotifier.info("Language server started at $port.")
    }
}


