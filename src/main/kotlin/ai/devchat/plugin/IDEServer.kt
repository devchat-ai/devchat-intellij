package ai.devchat.plugin

import ai.devchat.common.Log
import ai.devchat.common.Notifier
import ai.devchat.common.PathUtils
import ai.devchat.storage.CONFIG
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.navigation.actions.GotoTypeDeclarationAction
import com.intellij.lang.Language
import com.intellij.lang.annotation.HighlightSeverity.INFORMATION
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.awt.Point
import java.io.File
import java.net.ServerSocket
import kotlin.reflect.full.memberFunctions


const val START_PORT: Int = 31800


@Serializable
data class ReqLocation(val abspath: String, val line: Int, val character: Int)
@Serializable
data class Position(val line: Int, val character: Int)
@Serializable
data class Range(val start: Position, val end: Position)
@Serializable
data class Location(val abspath: String, val range: Range)
@Serializable
data class LocationWithText(val abspath: String, val range: Range, val text: String)
@Serializable
data class SymbolNode(val name: String?, val kind: String, val range: Range, val children: List<SymbolNode>)
@Serializable
data class Action(val className: String, val familyName: String, val text: String)
@Serializable
data class Issue(val location: Location, val text: String, val severity: String, val description: String, val action: Action?)
@Serializable
data class Result<T>(
    val result: T? = null
)

class IDEServer(private var project: Project) {
    private var server: ApplicationEngine? = null

    fun start() {
        ideServerPort = getAvailablePort(START_PORT)
        server = embeddedServer(Netty, port= ideServerPort!!) {
            install(CORS) {
                anyHost()
                allowSameOrigin = true
                allowCredentials = true
                allowNonSimpleContentTypes = true
            }
            install(ContentNegotiation) {
                json()
            }
            routing {
                post("/find_def_locations") {
                    val body: ReqLocation = call.receive()
                    val definitions = withContext(Dispatchers.IO)  {
                        project.findDefinitions(body.abspath, body.line, body.character)
                    }
                    call.respond(Result(definitions))
                }


                post("/references") {
                    val body: ReqLocation = call.receive()
                    val references = withContext(Dispatchers.IO)  {
                        project.findReferences(body.abspath, body.line, body.character)
                    }
                    call.respond(Result(references))
                }

                post("/get_document_symbols") {
                    val body = call.receive<Map<String, String>>()
                    val path = body["abspath"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest, "Missing or invalid parameters"
                    )
                    val symbols = withContext(Dispatchers.IO)  { project.findSymbols(path) }
                    call.respond(Result(symbols))
                }

                post("/find_type_def_locations") {
                    val body: ReqLocation = call.receive()
                    val typeDef = withContext(Dispatchers.IO)  {
                        val psiFile = project.getPsiFile(body.abspath)
                        val editor = project.getEditorForFile(psiFile)
                        val offset = project.computeOffset(psiFile, body.line, body.character)
                        findTypeDefinition(editor, offset)
                    }
                    call.respond(Result(typeDef))
                }

                post("/ide_language") {
                    call.respond(Result(CONFIG["language"] as? String))
                }

                post("/ide_name") {
                    call.respond(Result("intellij"))
                }

                get("/current_file_info") {
                    val file: VirtualFile = project.getCurrentFile()
                    call.respond(Result(mapOf(
                        "path" to file.path,
                        "extension" to file.extension,
                    )))
                }

                post("/get_diagnostics_in_range") {
                    val body = call.receive<Map<String, String>>()
                    val fileName = body["fileName"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest, "Missing or invalid parameters"
                    )
                    withContext(Dispatchers.IO)  {
                        val sonarRuleKeyRegex = "'([^':]+:[^':]+)'".toRegex()
                        val psiFile = project.getPsiFile(fileName)
                        val editor = project.getEditorForFile(psiFile)
                        val document = editor.document
                        val startLine = body["startLine"]?.toIntOrNull() ?: 0
                        val endLine = body["endLine"]?.toIntOrNull() ?: (document.lineCount - 1)
                        val startOffset = project.computeOffset(psiFile, startLine, 0)
                        val endOffset = project.computeOffset(psiFile, endLine, null)
                        val issues = mutableListOf<Issue>()
                        val highlightInfoProcessor = { hi: HighlightInfo ->
                            val sonarAction: IntentionAction? = hi.findRegisteredQuickFix { descriptor, _ ->
                                val action = descriptor.action
                                if (
                                    action.familyName.contains("SonarLint")
                                    || action.text.contains("SonarLint")
                                ) {
                                    action
                                } else null
                            }
                            val action = if (sonarAction == null) null else Action(
                                className = sonarAction.javaClass.name,
                                familyName = sonarAction.familyName,
                                text = sonarAction.text
                            )
                            issues.add(Issue(
                                location= Location(abspath = fileName, range = editor.range(startOffset, endOffset)),
                                text=hi.text,
                                description = hi.description,
                                severity = hi.severity.toString(),
                                action = action
                            ))
                            true
                        }
                        ApplicationManager.getApplication().invokeAndWait {
                            DaemonCodeAnalyzerEx.processHighlights(
                                document,
                                project,
                                INFORMATION,
                                startOffset,
                                endOffset,
                                highlightInfoProcessor
                            )
                        }
                        call.respond(Result(issues.map {issue ->
                            val source = when {
                                issue.action?.text?.contains("SonarLint") == true -> "sonar"
                                else -> "unknown"
                            }
                            val sonarRuleKey = issue.action?.text?.let{
                                sonarRuleKeyRegex.find(it)?.groups?.get(1)?.value
                            }
                            "${issue.description} <<$source:$sonarRuleKey>>"
                        }))
                    }
                }

                post("/get_extension_tools_path") {
                    call.respond(Result(PathUtils.toolsPath))
                }

                post("/get_collapsed_code") {
                    val body = call.receive<Map<String, String>>()
                    val fileName = body["fileName"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest, "Missing or invalid parameters"
                    )
                    call.respond(Result(project.getDocument(fileName).text))
                }

                post("/registered_languages") {
                    call.respond(Result(Language.getRegisteredLanguages().map { it.id }))
                }

                post("/get_selected_range") {
                    var editor: Editor? = null
                    ApplicationManager.getApplication().invokeAndWait {
                        editor = FileEditorManager.getInstance(project).selectedTextEditor
                    }
                    editor?.let {
                        call.respond(Result(it.selection()))
                    } ?: call.respond(HttpStatusCode.NoContent)
                }
                post("/get_visible_range") {
                    var editor: Editor? = null
                    ApplicationManager.getApplication().invokeAndWait {
                        editor = FileEditorManager.getInstance(project).selectedTextEditor
                    }
                    editor?.let {
                        call.respond(Result(it.visibleRange()))
                    } ?: call.respond(HttpStatusCode.NoContent)
                }
                post("/diff_apply") {
                    val body = call.receive<Map<String, String>>()
                    val filePath: String? = body["filepath"]
                    var content: String? = body["content"]
                    if (content.isNullOrEmpty() && !filePath.isNullOrEmpty()) {
                        content = File(filePath).readText()
                    }
                    if (content.isNullOrEmpty()) {
                        content = ""
                    }
                    var editor: Editor? = null
                    ApplicationManager.getApplication().invokeAndWait {
                        editor = FileEditorManager.getInstance(project).selectedTextEditor
                    }
                    editor?.diffWith(content)
                    call.respond(Result(true))
                }
                post("/ide_logging") {
                    val body = call.receive<Map<String, String>>()
                    val level = body["level"]
                    val message = body["message"]
                    // level must be one of "info", "warn", "error", "debug"
                    Log::class.memberFunctions.find { it.name == level }?.let{
                        it.call(Log, message)
                        call.respond(Result(true))
                    } ?: call.respond(Result(false))
                }
            }
        }

        // Register listener to stop the server when project closed
        ProjectManager.getInstance().addProjectManagerListener(
            project, object: ProjectManagerListener {
                override fun projectClosed(project: Project) {
                    super.projectClosed(project)
                    Notifier.info("Stopping IDE server...")
                    server?.stop(1_000, 2_000)
                }
            }
        )

        server?.start(wait = false)
        Notifier.info("IDE server started at $ideServerPort.")
    }
}

fun Editor.range(startOffset: Int, endOffset: Int): Range {
    var startPosition: LogicalPosition? = null
    var endPosition: LogicalPosition? = null
    ApplicationManager.getApplication().invokeAndWait {
        startPosition = this.offsetToLogicalPosition(startOffset)
        endPosition = this.offsetToLogicalPosition(endOffset)
    }
    return Range(
        start = Position(startPosition?.line ?: -1, startPosition?.column ?: -1),
        end = Position(endPosition?.line ?: -1, endPosition?.column ?: -1),
    )
}

fun Editor.selection(): LocationWithText {
    val selectionModel = this.selectionModel
    var startPosition: LogicalPosition? = null
    var endPosition: LogicalPosition? = null
    var selectedText: String? = null
    ApplicationManager.getApplication().invokeAndWait {
        if (selectionModel.hasSelection()) {
            val startOffset = selectionModel.selectionStart
            val endOffset = selectionModel.selectionEnd
            startPosition = this.offsetToLogicalPosition(startOffset)
            endPosition = this.offsetToLogicalPosition(endOffset)
            selectedText = selectionModel.selectedText
        }
    }
    val virtualFile = FileDocumentManager.getInstance().getFile(document)
    return LocationWithText(
        virtualFile?.path ?: "", Range(
            start = Position(startPosition?.line ?: -1, startPosition?.column ?: -1),
            end = Position(endPosition?.line ?: -1, endPosition?.column ?: -1),
        ), selectedText ?: ""
    )
}

fun Editor.visibleRange(): LocationWithText {
    var firstVisibleLine = 0
    var lastVisibleLine = 0
    var lastVisibleColumn = 0
    var visibleText = ""
    ApplicationManager.getApplication().invokeAndWait {
        val visibleArea = scrollingModel.visibleArea
        firstVisibleLine = xyToLogicalPosition(Point(visibleArea.x, visibleArea.y)).line
        lastVisibleLine = xyToLogicalPosition(Point(visibleArea.x, visibleArea.y + visibleArea.height)).line
        lastVisibleLine = minOf(lastVisibleLine, document.lineCount - 1)
        val startOffset = document.getLineStartOffset(firstVisibleLine)
        val endOffset = document.getLineEndOffset(lastVisibleLine)
        visibleText = document.getText(TextRange.create(startOffset, endOffset))
        lastVisibleColumn = offsetToLogicalPosition(endOffset).column
    }

    val virtualFile = FileDocumentManager.getInstance().getFile(document)
    return LocationWithText(
       virtualFile?.path ?: "", Range(
            start = Position(firstVisibleLine, 0),
            end = Position(lastVisibleLine, lastVisibleColumn),
        ), visibleText
    )
}

fun Editor.diffWith(newText: String) {
    ApplicationManager.getApplication().invokeLater {
        val dialog = DiffViewerDialog(this, newText)
        dialog.show()
    }
}

fun getAvailablePort(startPort: Int): Int {
    var port = startPort
    while (true) {
        try {
            ServerSocket(port).use { return port }
        } catch (ex: Exception) {
            port++
        }
    }
}

fun Project.getPsiFile(filePath: String): PsiFile = ReadAction.compute<PsiFile, Throwable> {
    val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(File(filePath))
    PsiManager.getInstance(this).findFile(virtualFile!!)
}

fun Project.getDocument(filePath: String): Document = ReadAction.compute<Document, Throwable> {
    LocalFileSystem.getInstance().findFileByIoFile(File(filePath))?.let {
        FileDocumentManager.getInstance().getDocument(it)
    }
}

fun Project.getCurrentFile(): VirtualFile = ReadAction.compute<VirtualFile, Throwable> {
    val editor: Editor? = FileEditorManager.getInstance(this).selectedTextEditor
    editor?.document?.let { document ->
        FileDocumentManager.getInstance().getFile(document)
    }
}

fun Project.computeOffset(
    psiFile: PsiFile,
    lineNumber: Int?,
    columnIndex: Int?,
): Int = ReadAction.compute<Int, Throwable> {
    if (lineNumber == null) return@compute -1
    val document = PsiDocumentManager.getInstance(this).getDocument(psiFile)!!
    if (columnIndex == null) document.getLineEndOffset(lineNumber)
    else document.getLineStartOffset(lineNumber) + columnIndex
}

fun Project.getEditorForFile(psiFile: PsiFile): Editor {
    val document = PsiDocumentManager.getInstance(this).getDocument(psiFile)
    var editor: Editor? = null
    ApplicationManager.getApplication().invokeAndWait {
        editor = EditorFactory.getInstance().createEditor(document!!, this)
    }
    return editor!!
}

fun PsiElement.toSymbolNode(): List<SymbolNode> {
    val range = this.getRange()
    return if (this is PsiNamedElement && this.name != null && range != null) {
        listOf(SymbolNode(
            this.name,
            this.javaClass.name,
            range,
            children = this.children.flatMap { it.toSymbolNode() }
        ))
    } else {
        this.children.flatMap { it.toSymbolNode() }
    }
}

fun PsiElement.getRange(): Range? {
    val document = PsiDocumentManager.getInstance(this.project).getDocument(this.containingFile)

    fun calculatePosition(offset: Int): Position {
        // line numbers are 0-based
        val line = document!!.getLineNumber(offset)
        // verifying the line number correctness
        if (line < 0 || line >= document.lineCount) {
            throw RuntimeException("Error locating element: Got an invalid line number $line")
        }
        val lineStartOffset = document.getLineStartOffset(line)
        val column = offset - lineStartOffset
        return Position(line, column)
    }

    return try {
        Range(calculatePosition(this.startOffset), calculatePosition(this.endOffset))
    } catch (e: Exception) {
        Log.warn(e.toString())
        null
    }
}

fun PsiElement.getLocation(): Location? {
    return this.getRange()?.let { Location(this.containingFile.virtualFile.path, it)}
}

fun Project.findReferences(
    filePath: String,
    lineNumber: Int,
    columnIndex: Int,
): List<Location> = ReadAction.compute<List<Location>, Throwable> {
    val psiFile = this.getPsiFile(filePath)
    val offset = this.computeOffset(psiFile, lineNumber, columnIndex)
    ProgressManager.getInstance().runProcess(Computable {
        PsiTreeUtil.findElementOfClassAtOffset(
            psiFile, offset,  PsiNamedElement::class.java, false
        )?.let {ele ->
            ReferencesSearch.search(ele).mapNotNull {it.element.getLocation()}
        }.orEmpty()
    }, EmptyProgressIndicator())
}

fun Project.findDefinitions(
    filePath: String,
    lineNumber: Int,
    columnIndex: Int,
): List<Location> = ReadAction.compute<List<Location>, Throwable> {
    val psiFile = this.getPsiFile(filePath)
    val offset = this.computeOffset(psiFile, lineNumber, columnIndex)
    ProgressManager.getInstance().runProcess(Computable {
        val ref = psiFile.findReferenceAt(offset)
        if (ref is PsiPolyVariantReference) {
            ref.multiResolve(false).mapNotNull { it.element?.getLocation() }
        } else listOfNotNull(ref?.resolve()?.getLocation())
    }, EmptyProgressIndicator())
}

fun Project.findSymbols(
    filePath: String,
    lineNumber: Int? = null,
    columnIndex: Int? = null,
): List<SymbolNode>? = ReadAction.compute<List<SymbolNode>, Throwable> {
    val psiFile = this.getPsiFile(filePath)
    val offset = this.computeOffset(psiFile, lineNumber, columnIndex)
    if (offset == -1) {
        psiFile.toSymbolNode()
    }

    PsiTreeUtil.findElementOfClassAtOffset(
        psiFile, offset,  PsiNamedElement::class.java, false
    )?.toSymbolNode() ?: listOf()
}

fun findTypeDefinition(
    editor: Editor,
    offset: Int,
): List<Location> = ReadAction.compute<List<Location>, Throwable> {
    GotoTypeDeclarationAction.findSymbolType(editor, offset)?.let {
        listOfNotNull(it.getLocation())
    }.orEmpty()
}

var ideServerPort: Int? = null
