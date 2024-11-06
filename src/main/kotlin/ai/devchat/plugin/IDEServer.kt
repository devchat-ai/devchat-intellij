package ai.devchat.plugin

import ai.devchat.common.IDEUtils.runInEdtAndGet
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
import com.intellij.openapi.Disposable
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


@Serializable
data class ReqLocation(val abspath: String, val line: Int, val character: Int)
@Serializable
data class DiffApplyRequest(val filepath: String?, val content: String?, val autoedit: Boolean? = false)
@Serializable
data class Position(val line: Int = -1, val character: Int = -1)
@Serializable
data class Range(val start: Position = Position(), val end: Position = Position())
@Serializable
data class Location(val abspath: String, val range: Range)
@Serializable
data class LocationWithText(
    val abspath: String = "",
    val range: Range = Range(),
    val text: String = ""
)
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

class IDEServer(private var project: Project): Disposable {
    private var server: ApplicationEngine? = null
    private var isShutdownHookRegistered: Boolean = false
    var port: Int? = null

    fun start(): IDEServer {
        ServerSocket(0).use {
            port = it.localPort
        }
        server = embeddedServer(Netty, port= port!!) {
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
                    val definitions = try {
                        withContext(Dispatchers.IO) {
                            project.findDefinitions(body.abspath, body.line, body.character)
                        }
                    } catch (e: Exception) {
                        Log.warn(e.toString())
                        emptyList()
                    }
                    call.respond(Result(definitions))
                }

                post("/references") {
                    val body: ReqLocation = call.receive()
                    val references = try {
                        withContext(Dispatchers.IO)  {
                            project.findReferences(body.abspath, body.line, body.character)
                        }
                    } catch (e: Exception) {
                        Log.warn(e.toString())
                        emptyList()
                    }
                    call.respond(Result(references))
                }

                post("/get_document_symbols") {
                    val body = call.receive<Map<String, String>>()
                    val path = body["abspath"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest, "Missing or invalid parameters"
                    )
                    val symbols = try {
                        withContext(Dispatchers.IO)  { project.findSymbols(path) }
                    } catch (e: Exception) {
                        Log.warn(e.toString())
                        emptyList()
                    }
                    call.respond(Result(symbols))
                }

                post("/find_type_def_locations") {
                    val body: ReqLocation = call.receive()
                    val typeDef = try {
                        withContext(Dispatchers.IO)  {
                            val psiFile = project.getPsiFile(body.abspath)
                            val editor = project.getEditorForFile(psiFile)
                            val offset = project.computeOffset(psiFile, body.line, body.character)
                            findTypeDefinition(editor, offset)
                        }
                    } catch (e: Exception) {
                        Log.warn(e.toString())
                        emptyList()
                    }
                    call.respond(Result(typeDef))
                }

                post("/get_local_service_port") {
                    val devChatService = project.getService(DevChatService::class.java)
                    call.respond(Result(devChatService.localServicePort))
                }

                post("/ide_language") {
                    call.respond(Result(CONFIG["language"] as? String))
                }

                post("/ide_name") {
                    call.respond(Result("intellij"))
                }

                get("/current_file_info") {
                    val file: VirtualFile? = try {
                        project.getCurrentFile()
                    } catch (e: Exception) {
                        Log.warn(e.toString())
                        null
                    }
                    call.respond(Result(mapOf(
                        "path" to file?.path,
                        "extension" to file?.extension,
                    )))
                }

                post("/get_diagnostics_in_range") {
                    val body = call.receive<Map<String, String>>()
                    val fileName = body["fileName"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest, "Missing or invalid parameters"
                    )

                    withContext(Dispatchers.IO)  {
                        val sonarRuleKeyRegex = "'([^':]+:[^':]+)'".toRegex()
                        val issues = mutableListOf<Issue>()
                        try {
                            val psiFile = project.getPsiFile(fileName)
                            val editor = project.getEditorForFile(psiFile)
                            val document = editor.document
                            val startLine = body["startLine"]?.toIntOrNull() ?: 0
                            val endLine = body["endLine"]?.toIntOrNull() ?: (document.lineCount - 1)
                            val startOffset = project.computeOffset(psiFile, startLine, 0)
                            val endOffset = project.computeOffset(psiFile, endLine, null)
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
                                issues.add(
                                    Issue(
                                        location = Location(
                                            abspath = fileName,
                                            range = editor.range(startOffset, endOffset)
                                        ),
                                        text = hi.text,
                                        description = hi.description,
                                        severity = hi.severity.toString(),
                                        action = action
                                    )
                                )
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
                        } catch (e: Exception) {
                            Log.warn(e.toString())
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
                    val content: String = try {
                        project.getDocument(fileName).text
                    } catch (e: Exception) {
                        Log.warn(e.toString())
                        ""
                    }
                    call.respond(Result(content))
                }

                post("/registered_languages") {
                    call.respond(Result(Language.getRegisteredLanguages().map { it.id }))
                }

                post("/select_range") {
                    val body = call.receive<Map<String, String>>()
                    val fileName = body["fileName"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest, "Missing or invalid parameters"
                    )
                    val startLine = body["startLine"]?.toIntOrNull()
                    val endLine = body["endLine"]?.toIntOrNull()
                    val startColumn = body["startColumn"]?.toIntOrNull() ?: 0
                    val endColumn = body["endColumn"]?.toIntOrNull()
                    val result = try {
                        val psiFile = project.getPsiFile(fileName)
                        val editor = project.openFile(psiFile)
                        runInEdtAndGet {
                            if (startLine == null || endLine == null || startLine < 0 || endLine < 0) {
                                editor.selectionModel.removeSelection()
                            } else {
                                val startOffset = project.computeOffset(psiFile, startLine, startColumn)
                                val endOffset = project.computeOffset(psiFile, endLine, endColumn)
                                editor.selectionModel.setSelection(startOffset, endOffset)
                            }
                            true
                        }
                    } catch (e: Exception) {
                        Log.warn(e.toString())
                        false
                    }
                    call.respond(Result(result))
                }
                post("/get_selected_range") {
                    val result = try {
                        var editor: Editor? = null
                        ApplicationManager.getApplication().invokeAndWait {
                            editor = FileEditorManager.getInstance(project).selectedTextEditor
                        }
                        editor?.selection() ?: LocationWithText()
                    } catch (e: Exception) {
                        Log.warn(e.toString())
                        LocationWithText()
                    }
                    call.respond(Result(result))
                }
                post("/get_visible_range") {
                    val result = try {
                        var editor: Editor? = null
                        ApplicationManager.getApplication().invokeAndWait {
                            editor = FileEditorManager.getInstance(project).selectedTextEditor
                        }
                        editor?.visibleRange() ?: LocationWithText()
                    } catch (e: Exception) {
                        Log.warn(e.toString())
                        LocationWithText()
                    }
                    call.respond(Result(result))
                }
                post("/diff_apply") {
                    val body: DiffApplyRequest = call.receive()
                    val filePath: String? = body.filepath
                    val content = body.content.takeUnless { it.isNullOrEmpty() }
                        ?: filePath?.takeUnless { it.isEmpty() }?.let { File(it).readText() }
                        ?: ""
                    val autoEdit: Boolean = body.autoedit ?: false
                    var editor: Editor? = null
                    ApplicationManager.getApplication().invokeAndWait {
                        editor = FileEditorManager.getInstance(project).selectedTextEditor
                    }
                    editor?.diffWith(content, autoEdit)
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

        // Register shutdown hook
        if (!isShutdownHookRegistered) {
            Runtime.getRuntime().addShutdownHook(Thread { stop() })
            isShutdownHookRegistered = true
        }

        server?.start(wait = false)
        Notifier.info("IDE server started at $port.")
        return this
    }

    fun stop() {
        Log.info("Stopping IDE server...")
        server?.stop(1_000, 2_000)
    }

    override fun dispose() {
        stop()
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

fun Editor.diffWith(newText: String, autoEdit: Boolean) {
    ApplicationManager.getApplication().invokeLater {
        val dialog = DiffViewerDialog(this, newText, autoEdit)
        dialog.show()
    }
}

fun Project.getPsiFile(filePath: String): PsiFile = runInEdtAndGet {
    ReadAction.compute<PsiFile, Throwable> {
        val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(File(filePath))
        PsiManager.getInstance(this).findFile(virtualFile!!)
    }
}

fun Project.getDocument(filePath: String): Document = runInEdtAndGet {
    ReadAction.compute<Document, Throwable> {
        LocalFileSystem.getInstance().findFileByIoFile(File(filePath))?.let {
            FileDocumentManager.getInstance().getDocument(it)
        }
    }
}

fun Project.getCurrentFile(): VirtualFile? = runInEdtAndGet {
    ReadAction.compute<VirtualFile, Throwable> {
        val editor: Editor? = FileEditorManager.getInstance(this).selectedTextEditor
        editor?.document?.let { document ->
            FileDocumentManager.getInstance().getFile(document)
        }
    }
}

fun Project.computeOffset(
    psiFile: PsiFile,
    lineNumber: Int?,
    columnIndex: Int?,
): Int = ReadAction.compute<Int, Throwable> {
    if (lineNumber == null) return@compute -1
    val document = PsiDocumentManager.getInstance(this).getDocument(psiFile)!!
    val lineEndOffset = document.getLineEndOffset(lineNumber)
    val lineStartOffset = document.getLineStartOffset(lineNumber)
    if (columnIndex == null) lineEndOffset else (lineStartOffset + columnIndex).coerceIn(lineStartOffset,lineEndOffset)
}

fun Project.getEditorForFile(psiFile: PsiFile): Editor {
    val document = PsiDocumentManager.getInstance(this).getDocument(psiFile)
    var editor: Editor? = null
    ApplicationManager.getApplication().invokeAndWait {
        editor = EditorFactory.getInstance().createEditor(document!!, this)
    }
    return editor!!
}

fun Project.openFile(psiFile: PsiFile): Editor {
    var editor: Editor? = null
    ApplicationManager.getApplication().invokeAndWait {
        val fileEditorManager = FileEditorManager.getInstance(this)
        val currentEditor = fileEditorManager.selectedTextEditor
        editor =  currentEditor?.takeIf {
            val virtualFile = FileDocumentManager.getInstance().getFile(it.document)
            virtualFile?.path == psiFile.virtualFile.path
        }
        if (editor == null) {
            fileEditorManager.openFile(psiFile.virtualFile, true)
            editor = fileEditorManager.selectedTextEditor
        }
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
