package ai.devchat.plugin.completion.agent

import ai.devchat.common.IDEUtils.findAccessibleVariables
import ai.devchat.common.IDEUtils.findCalleeInParent
import ai.devchat.common.IDEUtils.foldTextOfLevel
import ai.devchat.common.IDEUtils.runInEdtAndGet
import ai.devchat.common.Log
import com.intellij.psi.PsiFile

const val MAX_CONTEXT_TOKENS = 6000
const val LINE_SEPARATOR = '\n'


fun String.tokenCount(): Int {
    var count = 0
    var isPrevWhiteSpace = true
    for (char in this) {
        if (char.isWhitespace()) {
            isPrevWhiteSpace = true
        } else {
            if (isPrevWhiteSpace) count++
            isPrevWhiteSpace = false
        }
    }
    return count
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
    val filepath: String = file.virtualFile.path
    val content: String = file.text
    // TODO: get comment prefix for different languages
    private val commentPrefix: String = "//"
    private var tokenCount: Int = 0

    private fun buildFileContext(): Pair<String, String> {
        val maxTokens = MAX_CONTEXT_TOKENS * 0.35

        val maxPrefixTokens = (maxTokens * 0.7).toInt()
        var prefixTokens = 0
        val prefixStart = content.lineSequenceReversed(offset).takeWhile {(_, line) ->
            val numTokens = line.tokenCount()
            if (prefixTokens + numTokens > maxPrefixTokens) return@takeWhile false
            prefixTokens += numTokens
            true
        }.lastOrNull()?.first?.first ?: 0
        tokenCount += prefixTokens

        val maxSuffixTokens = maxTokens - prefixTokens
        var suffixTokens = 0
        val suffixEnd = content.lineSequence(offset).takeWhile {(_, line) ->
            val numTokens = line.tokenCount()
            if (suffixTokens + numTokens > maxSuffixTokens) return@takeWhile false
            suffixTokens += numTokens
            true
        }.lastOrNull()?.first?.last ?: content.length
        tokenCount += suffixTokens

        return Pair(
            content.substring(prefixStart, offset),
            content.substring(offset, suffixEnd)
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
                ?.joinToString(separator = "") {
                    "$commentPrefix<filename>call function define:\n\n${it.filepath}\n\n${it.content}\n\n\n\n"
                } ?: ""
        }
    }

    private fun buildSymbolsContext(): String {
        return runInEdtAndGet {
            file.findElementAt(offset)
                ?.findAccessibleVariables()
                ?.filter { it.typeDeclaration.element.containingFile.virtualFile.path != filepath }
                ?.map {
                    val typeElement = it.typeDeclaration.element
                    it.symbol.name to CodeSnippet(
                        typeElement.containingFile.virtualFile.path,
                        if (it.typeDeclaration.isProjectContent) {
                            typeElement.foldTextOfLevel(2)
                        } else {
                            typeElement.text.lines().first() + "..."
                        }
                    )
                }
                ?.takeWhile { checkAndUpdateTokenCount(it.second) }
                ?.joinToString(separator = "") {(name, snippet) ->
                    val commentedContent = snippet.content.lines().joinToString(LINE_SEPARATOR.toString()) {
                        "$commentPrefix $it"
                    }
                    "$commentPrefix Symbol type definition:\n\n" +
                            "$commentPrefix <symbol>${name}\n\n" +
                            "$commentPrefix <filename>${snippet.filepath}\n\n" +
                            "$commentPrefix <definition>\n$commentedContent\n\n\n\n"
                } ?: ""
        }
    }

    fun createPrompt(model: String?): String {
        val (prefix, suffix) = buildFileContext()
        val extras: String = listOf(
//            taskDescriptionContextWithCommentPrefix,
//            neighborFileContext,
            buildCalleeDefinitionsContext(),
            buildSymbolsContext(),
//            similarBlockContext,
//            gitDiffContext,
        ).joinToString("")
        Log.info("Extras completion context:\n$extras")
        return  if (!model.isNullOrEmpty() && model.contains("deepseek"))
            "<｜fim▁begin｜>$extras<filename>$filepath\n\n$prefix<｜fim▁hole｜>$suffix<｜fim▁end｜>"
        else
            "<fim_prefix>$extras<filename>$filepath\n\n$prefix<fim_suffix>$suffix<fim_middle>"
    }
}