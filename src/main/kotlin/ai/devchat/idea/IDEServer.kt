package ai.devchat.idea

import ai.devchat.common.ProjectUtils
import ai.devchat.idea.balloon.DevChatNotifier
import ai.devchat.idea.settings.DevChatSettingsState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.*
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
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
): List<SymbolLocation> = ReadAction.compute<List<SymbolLocation>, Throwable> {
    val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(File(filePath))
    val psiFile = PsiManager.getInstance(this).findFile(virtualFile!!)
    val document = PsiDocumentManager.getInstance(this).getDocument(psiFile!!)
    val offset = document!!.getLineStartOffset(lineNumber - 1) + columnIndex - 1
    ProgressManager.getInstance().runProcess(Computable {
        PsiTreeUtil.findElementOfClassAtOffset(
            psiFile, offset,  PsiNamedElement::class.java, false
        )?.let {ele ->
            ReferencesSearch.search(ele).map {SymbolLocation.fromPsiElement(it.element)}
        }.orEmpty()
    }, EmptyProgressIndicator())
}

fun Project.findDefinitions(
    filePath: String,
    lineNumber: Int,
    columnIndex: Int,
): List<SymbolLocation> = ReadAction.compute<List<SymbolLocation>, Throwable> {
    val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(File(filePath))
    val psiFile = PsiManager.getInstance(this).findFile(virtualFile!!)
    val document = PsiDocumentManager.getInstance(this).getDocument(psiFile!!)
    val offset = document!!.getLineStartOffset(lineNumber - 1) + columnIndex - 1
    ProgressManager.getInstance().runProcess(Computable {
        listOfNotNull(psiFile.findReferenceAt(offset)?.resolve()?.let {
            SymbolLocation.fromPsiElement(it)
        })
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

@Serializable
data class Result(val result: String)

@Serializable
data class SymbolLocation(val name: String, val abspath: String, val line: Int, val character: Int) {
    companion object {
        fun fromPsiElement(element: PsiElement): SymbolLocation = ReadAction.compute<SymbolLocation, Throwable> {
            val document = PsiDocumentManager.getInstance(element.project).getDocument(element.containingFile)
            // line numbers are 0-based
            val line = document!!.getLineNumber(element.textOffset) + 1
            // verifying the line number correctness
            if (line <= 0 || line > document.lineCount) {
                throw RuntimeException("Error locating element: Got an invalid line number $line")
            }
            val lineStartOffset = document.getLineStartOffset(line - 1)
            val column = element.textOffset - lineStartOffset + 1
            SymbolLocation(element.text, element.containingFile.virtualFile.path, line, column)
        }
    }
}

class IDEServer(private var project: Project) {
    private var server: ApplicationEngine? = null

    fun start() {
        val port = findAvailablePort(START_PORT)
        ProjectUtils.ideServerPort = port
        server = embeddedServer(Netty, port=port) {
            install(ContentNegotiation) {
                json()
            }
            routing {
                get("/definitions") {
                    val (path, line, column) = getLocParams(call.parameters) ?: return@get call.respond(
                        HttpStatusCode.BadRequest, "Missing or invalid parameters"
                    )
                    val definitions = withContext(Dispatchers.IO)  { project.findDefinitions(path, line, column) }
                    call.respond(definitions)
                }


                get("/references") {
                    val (path, line, column) = getLocParams(call.parameters) ?: return@get call.respond(
                        HttpStatusCode.BadRequest, "Missing or invalid parameters"
                    )
                    val references = withContext(Dispatchers.IO)  { project.findReferences(path, line, column) }
                    call.respond(references)
                }

                post("/ide_language") {
                    call.respond(Result(DevChatSettingsState.instance.language))
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


