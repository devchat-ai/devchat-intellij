package ai.devchat.idea

import ai.devchat.common.Log
import ai.devchat.idea.balloon.DevChatNotifier
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.DefinitionsScopedSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.utils.vfs.getPsiFile
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

fun Project.findReferences(
    filePath: String,
    lineNumber: Int,
    columnIndex: Int,
): List<PsiReference> = ReadAction.compute<List<PsiReference>, Throwable> {
    val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(File(filePath))
    val psiFile = PsiManager.getInstance(this).findFile(virtualFile!!)
    val document = PsiDocumentManager.getInstance(this).getDocument(psiFile!!)
    val offset = document!!.getLineStartOffset(lineNumber - 1) + columnIndex - 1
    ProgressManager.getInstance().runProcess(Computable {
        PsiTreeUtil.findElementOfClassAtOffset(
            psiFile, offset,  PsiNamedElement::class.java, false
        )?.let {
            ReferencesSearch.search(it.navigationElement).findAll().toList()
        }.orEmpty()
    }, EmptyProgressIndicator())
}

fun Project.findDefinitions(
    filePath: String,
    lineNumber: Int,
    columnIndex: Int,
): List<PsiElement> = ReadAction.compute<List<PsiElement>, Throwable> {
    val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(File(filePath))
    val psiFile = PsiManager.getInstance(this).findFile(virtualFile!!)
    val document = PsiDocumentManager.getInstance(this).getDocument(psiFile!!)
    val offset = document!!.getLineStartOffset(lineNumber - 1) + columnIndex - 1
    ProgressManager.getInstance().runProcess(Computable {
        listOfNotNull(psiFile.findReferenceAt(offset)?.resolve())
    }, EmptyProgressIndicator())
}

fun getLocParams(parameters: Parameters): Triple<String, Int, Int>? {
    val path = parameters["abspath"]
    val line = parameters["line"]?.toIntOrNull()
    val column = parameters["character"]?.toIntOrNull()

    if (path == null || line == null || column == null) {
        return null
    }

    return Triple(path, line, column)
}

class LanguageServer(private var project: Project) {
    private var server: ApplicationEngine? = null

    fun start() {
        val port = findAvailablePort(START_PORT)
        server = embeddedServer(Netty, port=port) {
            routing {
                get("/definitions") {
                    val (path, line, column) = getLocParams(call.parameters) ?: return@get call.respond(
                        HttpStatusCode.BadRequest, "Missing or invalid parameters"
                    )
                    val elements = withContext(Dispatchers.IO)  { project.findDefinitions(path, line, column) }
                    call.respond(elements.joinToString("\n") { it.text })
                }


                get("/references") {
                    val (path, line, column) = getLocParams(call.parameters) ?: return@get call.respond(
                        HttpStatusCode.BadRequest, "Missing or invalid parameters"
                    )
                    val references = withContext(Dispatchers.IO)  { project.findReferences(path, line, column) }
                    call.respond(references.joinToString("\n") { it.element.parent.parent.text })
                }
            }
        }

        // Register listener to stop the server when project closed
        ProjectManager.getInstance().addProjectManagerListener(
            project, object: ProjectManagerListener {
                override fun projectClosed(project: Project) {
                    super.projectClosed(project)
                    DevChatNotifier.info("Stopping language server...")
                    server?.stop(1_000, 2_000)
                }
            }
        )

        server?.start(wait = false)
        DevChatNotifier.info("Language server started at $port.")
    }
}


