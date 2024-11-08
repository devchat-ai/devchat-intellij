package ai.devchat.plugin.completion.agent

import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiDocumentManager
import ai.devchat.common.Constants.LANGUAGE_COMMENT_PREFIX
import ai.devchat.common.IDEUtils.findAccessibleVariables
import ai.devchat.common.IDEUtils.findCalleeInParent
import ai.devchat.common.IDEUtils.foldTextOfLevel
import ai.devchat.common.IDEUtils.runInEdtAndGet
import ai.devchat.common.Log
import ai.devchat.storage.RecentFilesTracker
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilCore.getPsiFile
import ai.devchat.storage.CONFIG
import com.intellij.openapi.application.ApplicationManager
import java.util.concurrent.atomic.AtomicInteger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import java.util.concurrent.ConcurrentHashMap


val MAX_CONTEXT_TOKENS: Int
    get() = (CONFIG["complete_context_limit"] as? Int) ?: 6000
const val LINE_SEPARATOR = '\n'


fun String.tokenCount(): Int {
//    var count = 0
//    var isPrevWhiteSpace = true
//    for (char in this) {
//        if (char.isWhitespace()) {
//            isPrevWhiteSpace = true
//        } else {
//            if (isPrevWhiteSpace) count++
//            isPrevWhiteSpace = false
//        }
//    }
//    return count
    // use length as token count
    return this.length
}


fun String.lineSequence(offset: Int? = null) = sequence {
    val str = this@lineSequence
    var prev = offset ?: 0
    while (prev < str.length) {
        val cur =  str.indexOf(LINE_SEPARATOR, startIndex = prev).let {
            if (it == -1) str.length else it + 1
        }
        val range = prev until cur
        yield(Pair(range, str.substring(range)))
        prev = cur
    }
}

fun String.lineSequenceReversed(offset: Int? = null) = sequence {
    val str = this@lineSequenceReversed
    var prev = offset ?: str.length
    while (prev > 0) {
        val cur = if (prev <= 1) 0 else {
            str.lastIndexOf(
                LINE_SEPARATOR,
                startIndex = if (str[prev-1] == LINE_SEPARATOR) {
                    prev - 2
                } else {
                    prev - 1
                }
            ) + 1
        }
        val range = cur until prev
        yield(Pair(range, str.substring(range)))
        prev = cur
    }
}

data class CodeSnippet (
    val filepath: String,
    val content: String
)

class ContextBuilder(val file: PsiFile, val offset: Int) {
    private val CURSOR_MARKER = "<<<CURSOR>>>"
    private val foldedContentCache = ConcurrentHashMap<Int, Pair<String, Int>>()
    private val foldCounter = AtomicInteger(0)

    val filepath: String = file.virtualFile.path
    val content: String by lazy {
        ReadAction.compute<String, Throwable> {
            val psiDocumentManager = PsiDocumentManager.getInstance(file.project)
            val document = psiDocumentManager.getDocument(file)
            if (document != null) {
                psiDocumentManager.doPostponedOperationsAndUnblockDocument(document)
                document.text
            } else {
                file.text
            }
        }
    }
//    val content: String by lazy {
//        ReadAction.compute<String, Throwable> { file.text }
//    }
    private val commentPrefix: String = LANGUAGE_COMMENT_PREFIX[file.language.id.lowercase()] ?: "//"
    private var tokenCount: Int = 0

    private fun buildFileContext(): Pair<String, String> {
        val maxTokens = MAX_CONTEXT_TOKENS * 0.35
        val contentLines = content.lines()

        if (contentLines.size <= 1000 && content.tokenCount() > maxTokens) {
            val (foldedContent, markerOffset) = getFoldedContent()
            val (prefix, suffix) = buildContextFromFoldedContent(foldedContent, markerOffset)
            return adjustContextTokens(prefix, suffix)
        } else {
            val (prefix, suffix) = buildOriginalContext()
            return adjustContextTokens(prefix, suffix)
        }
    }

    private fun adjustContextTokens(prefix: String, suffix: String): Pair<String, String> {
        val totalTokens = prefix.tokenCount() + suffix.tokenCount()
        if (totalTokens <= MAX_CONTEXT_TOKENS) {
            return Pair(prefix, suffix)
        }

        val prefixTokens = prefix.tokenCount()
        val suffixTokens = suffix.tokenCount()

        return when {
            prefixTokens <= MAX_CONTEXT_TOKENS / 2 -> {
                val newSuffixLength = MAX_CONTEXT_TOKENS - prefixTokens
                Pair(prefix, suffix.take(newSuffixLength))
            }
            suffixTokens <= MAX_CONTEXT_TOKENS / 2 -> {
                val newPrefixLength = MAX_CONTEXT_TOKENS - suffixTokens
                Pair(prefix.takeLast(newPrefixLength), suffix)
            }
            else -> {
                val halfMaxTokens = MAX_CONTEXT_TOKENS / 2
                Pair(prefix.takeLast(halfMaxTokens), suffix.take(halfMaxTokens))
            }
        }
    }

    private fun getFoldedContent(): Pair<String, Int> {
        val foldId = foldCounter.getAndIncrement()
        return foldedContentCache.computeIfAbsent(foldId) {
            val contentWithMarker = insertCursorMarker(content, offset)
            val foldedContent = foldFunctions(contentWithMarker)
            val markerOffset = foldedContent.indexOf(CURSOR_MARKER)
            foldedContent.replace(CURSOR_MARKER, "") to markerOffset
        }
    }

    private fun insertCursorMarker(text: String, offset: Int): String {
        return text.substring(0, offset) + CURSOR_MARKER + text.substring(offset)
    }

    private fun foldFunctions(text: String): String {
        return ReadAction.compute<String, Throwable> {
            val psiFile = PsiDocumentManager.getInstance(file.project).getPsiFile(file.viewProvider.document!!)
            val foldInfoList = mutableListOf<FoldInfo>()
            val cursorOffset = text.indexOf(CURSOR_MARKER)
            val markerLength = CURSOR_MARKER.length

            psiFile?.accept(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (isFunctionElement(element)) {
                        val start = element.textRange.startOffset
                        val end = element.textRange.endOffset
                        val adjustedStart = adjustOffset(start, cursorOffset, markerLength)
                        val adjustedEnd = adjustOffset(end, cursorOffset, markerLength)

                        if (!elementContainsCursor(adjustedStart, adjustedEnd, cursorOffset)) {
                            val foldedText = element.foldTextOfLevel(1)
                            foldInfoList.add(FoldInfo(adjustedStart, adjustedEnd, foldedText))
                        }
                    } else {
                        super.visitElement(element)
                    }
                }
            })

            // 按照结束位置降序排序，确保从后向前替换
            foldInfoList.sortByDescending { it.end }

            val sb = StringBuilder(text)
            for (foldInfo in foldInfoList) {
                sb.replace(foldInfo.start, foldInfo.end, foldInfo.foldedText)
            }

            sb.toString()
        }
    }

    private data class FoldInfo(val start: Int, val end: Int, val foldedText: String)

    private fun adjustOffset(offset: Int, cursorOffset: Int, markerLength: Int): Int {
        return if (offset > cursorOffset) offset + markerLength else offset
    }

    private fun elementContainsCursor(start: Int, end: Int, cursorOffset: Int): Boolean {
        return cursorOffset in start until end
    }

    private fun isFunctionElement(element: PsiElement): Boolean {
        // 这里需要根据你的语言特性来判断是否为函数元素
        // 例如，可以检查元素的类型或结构
//        Log.info("elementType: ${element.node.elementType.toString()}")
        return element.node.elementType.toString() == "FUNCTION" ||
                element.node.elementType.toString() == "METHOD" ||
                element.node.elementType.toString() == "FUN"
    }

    private fun buildContextFromFoldedContent(foldedContent: String, markerOffset: Int): Pair<String, String> {
        return Pair(
            foldedContent.substring(0, markerOffset),
            foldedContent.substring(markerOffset, foldedContent.length)
        )
    }

    private fun buildOriginalContext(): Pair<String, String> {
        return Pair(
            content.substring(0, offset),
            content.substring(offset, content.length)
        )
    }

    private fun checkAndUpdateTokenCount(snippet: CodeSnippet): Boolean {
        val newCount = tokenCount + snippet.content.tokenCount()
        return (newCount <= MAX_CONTEXT_TOKENS).also { if (it) tokenCount = newCount }
    }

    private fun buildCalleeDefinitionsContext(): String {
        return runInEdtAndGet {
            file.findElementAt(offset)
                ?.findCalleeInParent()
                ?.flatMap { elements -> elements.filter { it.containingFile.virtualFile.path != filepath } }
                ?.map { CodeSnippet(it.containingFile.virtualFile.path, it.foldTextOfLevel(1)) }
                ?.takeWhile(::checkAndUpdateTokenCount)
                ?.joinToString(separator = "") {snippet ->
                    val commentedContent = snippet.content.lines()
                        .joinToString(LINE_SEPARATOR.toString())
                    "$commentPrefix Function call definition:\n\n" +
                            "$commentPrefix <filename>${snippet.filepath}\n\n" +
                            "$commentPrefix <definition>\n$commentedContent\n\n\n\n"
                } ?: ""
        }
    }

    private fun buildSymbolsContext(): String {
        return ApplicationManager.getApplication().runReadAction<String> {
            Log.info("Starting buildSymbolsContext")
            val element = file.findElementAt(offset)
            Log.info("Found element at offset: ${element?.text}")

            val variables = element?.findAccessibleVariables() ?: emptySequence()

            // 使用 toList() 来触发惰性序列的计算，确保在 Read Action 中完成
            val variablesList = variables.toList()
            val variablesCount = variablesList.size
            Log.info("Found $variablesCount accessible variables")

            val processedTypes = mutableSetOf<String>()
            val result = StringBuilder()

            variablesList.forEach { variable ->
                val typeElement = variable.typeDeclaration.element
                val isLocalType = typeElement.containingFile.virtualFile.path == filepath
                val typeText = limitTypeText(typeElement.text)
                val typeFilePath = typeElement.containingFile.virtualFile.path
                val typeKey = "${typeElement.text}:$typeFilePath"

                Log.info("Processing variable ${variable.symbol.name}")
                Log.info("Is local type: $isLocalType")
                if (isValidTypePath(typeFilePath)) {
                    if (!processedTypes.contains(typeKey)) {
                        if (!isLocalType) {
                            Log.info("Variable ${variable.symbol.name} type: $typeText")
                            Log.info("Actual type file: $typeFilePath")

                            processedTypes.add(typeKey)

                            val snippet = CodeSnippet(
                                typeFilePath,
                                if (variable.typeDeclaration.isProjectContent) {
                                    typeElement.foldTextOfLevel(2)
                                } else {
                                    typeElement.text.lines().first() + "..."
                                }
                            )

                            if (checkAndUpdateTokenCount(snippet)) {
                                Log.info("Adding context for type: ${typeText}")
                                val commentedContent = snippet.content.lines()
                                    .joinToString(LINE_SEPARATOR.toString())
                                result.append("$commentPrefix Symbol type definition:\n\n")
                                    .append("$commentPrefix <symbol>${variable.symbol.name}\n\n")
                                    .append("$commentPrefix <filename>${snippet.filepath}\n\n")
                                    .append("$commentPrefix <definition>\n$commentedContent\n\n\n\n")
                            } else {
                                Log.info("Skipping type ${variable.symbol.name} due to token limit")
                                return@forEach
                            }
                        } else {
                            Log.info("Skipping type ${variable.symbol.name} due to local definition")
                        }
                    } else {
                        Log.info("Skipping duplicate type: ${variable.symbol.name}")
                    }
                } else {
                    Log.info("Skipping invalid type path: ${typeFilePath}")
                }
            }

            Log.info("buildSymbolsContext result length: ${result.length}")
            result.toString()
        }
    }

    private fun isValidTypePath(path: String): Boolean {
        // 这里需要根据具体的项目结构和依赖管理方式来实现
        // 例如，可以检查路径是否在项目目录下，或者是否在已知的第三方依赖目录下
        // 以下是一个简单的示例实现
        val projectPath = file.project.basePath ?: return false
        return path.startsWith(projectPath)
        //return path.startsWith(projectPath) || path.contains("/.gradle/") || path.contains("/build/")
    }

    // 新增的辅助函数，用于限制类型文本的输出长度
    private fun limitTypeText(text: String, maxLines: Int = 5): String {
        val lines = text.lines()
        return when {
            lines.size <= maxLines -> text
            else -> {
                val firstLines = lines.take(maxLines / 2)
                val lastLines = lines.takeLast(maxLines / 2)
                (firstLines + "..." + lastLines).joinToString("\n")
            }
        }
    }

    private fun buildRecentFilesContext(): String {
        val project = file.project
        return runInEdtAndGet {
            project.getService(RecentFilesTracker::class.java).getRecentFiles().asSequence()
                .filter { it.isValid && !it.isDirectory && it.path != filepath }
                .map { CodeSnippet(it.path, getPsiFile(project, it).foldTextOfLevel(2)) }
                .filter { it.content.lines().count(String::isBlank) <= 50 }
                .takeWhile(::checkAndUpdateTokenCount)
                .joinToString(separator = "") {snippet ->
                    val commentedContent = snippet.content.lines()
                        .joinToString(LINE_SEPARATOR.toString())
                    "$commentPrefix Recently open file:\n\n" +
                            "$commentPrefix <filename>${snippet.filepath}\n\n" +
                            "$commentedContent\n\n\n\n"
                }
        }
    }

    fun createPrompt(model: String?): String {
        val (prefix, suffix) = buildFileContext()
        var currentTokenCount = prefix.tokenCount() + suffix.tokenCount()
        val maxAllowedTokens = (MAX_CONTEXT_TOKENS * 0.9).toInt()

        val extraContexts = mutableListOf<String>()

        Log.info("Current token count: $currentTokenCount")
        Log.info("Max allowed tokens: $maxAllowedTokens")
        if (currentTokenCount < maxAllowedTokens) {
            val contextBuilders = listOf(
                ::buildCalleeDefinitionsContext,
                ::buildSymbolsContext,
                ::buildRecentFilesContext
            )

            for (builder in contextBuilders) {
                val context = builder()
                val contextTokens = context.tokenCount()
                if (currentTokenCount + contextTokens <= maxAllowedTokens) {
                    extraContexts.add(context)
                    currentTokenCount += contextTokens
                } else {
                    break
                }
            }
        }

        val extras = extraContexts.joinToString("")

        return if (!model.isNullOrEmpty() && model.contains("deepseek")) {
            "<｜fim▁begin｜>$extras$commentPrefix<filename>$filepath\n\n$prefix<｜fim▁hole｜>$suffix<｜fim▁end｜>"
        } else if (!model.isNullOrEmpty() && model.contains("starcoder")) {
            "<fim_prefix>$extras$commentPrefix<filename>$filepath\n\n$prefix<fim_suffix>$suffix<fim_middle>"
        } else if (!model.isNullOrEmpty() && model.contains("codestral")) {
            "<s>[SUFFIX]$suffix[PREFIX]$extras$commentPrefix<filename>$filepath\n\n$prefix"
        } else {
            "<fim_prefix>$extras$commentPrefix<filename>$filepath\n\n$prefix<fim_suffix>$suffix<fim_middle>"
        }
    }
}