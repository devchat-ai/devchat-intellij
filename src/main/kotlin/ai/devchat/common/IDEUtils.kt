package ai.devchat.common

import com.intellij.codeInsight.navigation.actions.TypeDeclarationProvider
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.lang.folding.LanguageFolding
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.util.elementType
import com.intellij.psi.util.findParentInFile
import com.intellij.refactoring.suggested.startOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import kotlin.system.measureTimeMillis
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.codeInsight.navigation.actions.GotoTypeDeclarationAction
import com.intellij.openapi.fileEditor.FileEditorManager
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer


object IDEUtils {
    private const val MAX_CACHE_SIZE = 100
    private const val MAX_FOLD_CACHE_SIZE = 100 // 可以根据需要调整

    private data class CacheEntry(val filePath: String, val offset: Int, val element: SoftReference<SymbolTypeDeclaration>)

    private val variableCache = object : LinkedHashMap<String, CacheEntry>(MAX_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, CacheEntry>): Boolean {
            return size > MAX_CACHE_SIZE
        }
    }
    private val cacheLock = ReentrantReadWriteLock()

    private data class FoldCacheEntry(
        val foldedText: String,
        val elementPointer: SmartPsiElementPointer<PsiElement>,
        val elementLength: Int,
        val elementHash: Int
    )

    private val foldCache = object : LinkedHashMap<String, SoftReference<FoldCacheEntry>>(MAX_FOLD_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, SoftReference<FoldCacheEntry>>): Boolean {
            return size > MAX_FOLD_CACHE_SIZE
        }
    }

    fun <T> runInEdtAndGet(block: () -> T): T {
        val app = ApplicationManager.getApplication()
        if (app.isDispatchThread) {
            return block()
        }
        val future = CompletableFuture<T>()
        val latch = CountDownLatch(1)
        app.invokeLater {
            try {
                val result = block()
                future.complete(result)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            } finally {
                latch.countDown()
            }
        }
        latch.await()
        return future.get()
    }

    fun findCalleeInParent(element: PsiElement?): List<PsiElement>? {
        if (element == null) return null
        Log.info("Find callee in parent: ${element.elementType}: ${element.text.replace("\n", "\\n")}")
        val nearestCallExpression = element.findParentInFile(withSelf = true) {
            if (it is PsiFile) false else {
                it.elementType.toString() in Constants.CALL_EXPRESSION_ELEMENT_TYPE_NAMES
            }
        }

        if (nearestCallExpression == null) return null

        Log.info("Nearest call expression: ${nearestCallExpression.elementType}: ${nearestCallExpression.text.replace("\n", "\\n")}")

        val projectFileIndex = ProjectFileIndex.getInstance(element.project)
        val callee = nearestCallExpression.children.asSequence()
            .mapNotNull {child ->
                child.reference?.let{ref ->
                    if (ref is PsiPolyVariantReference) {
                        ref.multiResolve(false).mapNotNull { it.element }
                    } else listOfNotNull(ref.resolve())
                }?.filter {
                    val containingFile = it.containingFile?.virtualFile
                    containingFile != null && projectFileIndex.isInContent(containingFile)
                }
            }
            .firstOrNull {it.isNotEmpty()}

        if (callee == null) {
            Log.info("Callee not found")
        } else {
            Log.info("Callee: $callee")
        }

        return callee ?: findCalleeInParent(nearestCallExpression.parent)
    }

    fun PsiElement.findCalleeInParent(): Sequence<List<PsiElement>> {
        val projectFileIndex = ProjectFileIndex.getInstance(this.project)
        return generateSequence(this) { it.parent }
            .takeWhile { it !is PsiFile }
            .filter { it.elementType.toString() in Constants.CALL_EXPRESSION_ELEMENT_TYPE_NAMES }
            .mapNotNull { callExpression ->
                Log.info("Call expression: ${callExpression.elementType}: ${callExpression.text}")

                callExpression.children
                    .asSequence()
                    .mapNotNull { child ->
                        child.reference?.let { ref ->
                            if (ref is PsiPolyVariantReference) {
                                ref.multiResolve(false).mapNotNull { it.element }
                            } else {
                                listOfNotNull(ref.resolve())
                            }
                            .filter { resolved ->
                                resolved.containingFile.virtualFile?.let { file ->
                                    projectFileIndex.isInContent(file)
                                } == true
                            }
                        }
                    }
                    .firstOrNull { it.isNotEmpty() }
            }
    }


    private fun PsiElement.getTypeDeclaration(): PsiElement? = runBlocking(Dispatchers.IO) {
        ReadAction.compute<PsiElement?, Throwable> {
            TypeDeclarationProvider.EP_NAME.extensionList.asSequence()
                .mapNotNull { provider ->
                    provider.getSymbolTypeDeclarations(this@getTypeDeclaration)?.firstOrNull()
                }
                .firstOrNull()
        }
    }

    data class CodeNode(
        val element: PsiElement,
        val isProjectContent: Boolean,
    )
    data class SymbolTypeDeclaration(
        val symbol: PsiNameIdentifierOwner,
        val typeDeclaration: CodeNode
    )

    fun PsiElement.findAccessibleVariables(): Sequence<SymbolTypeDeclaration> {
        val projectFileIndex = ProjectFileIndex.getInstance(project)

        // 首先收集所有可能的变量
        val allVariables = sequence {
            var currentScope: PsiElement? = this@findAccessibleVariables
            while (currentScope != null && currentScope !is PsiFile) {
                val variablesInScope = PsiTreeUtil.findChildrenOfAnyType(
                    currentScope,
                    false,
                    PsiNameIdentifierOwner::class.java
                )

                for (variable in variablesInScope) {
                    if (isLikelyVariable(variable) && !variable.name.isNullOrEmpty() && variable.nameIdentifier != null) {
                        yield(variable)
                    }
                }

                currentScope = currentScope.parent
            }

            yieldAll(this@findAccessibleVariables.containingFile.children
                .asSequence()
                .filterIsInstance<PsiNameIdentifierOwner>()
                .filter { isLikelyVariable(it) && !it.name.isNullOrEmpty() && it.nameIdentifier != null })
        }.distinct()

        // 处理这些变量的类型，使用缓存
        return allVariables.mapNotNull { variable ->
            val cacheKey = "${variable.containingFile?.virtualFile?.path}:${variable.textRange.startOffset}"

            getCachedOrCompute(cacheKey, variable)
        }
    }

    private fun getCachedOrCompute(cacheKey: String, variable: PsiElement): SymbolTypeDeclaration? {
        cacheLock.read {
            variableCache[cacheKey]?.let { entry ->
                entry.element.get()?.let { cached ->
                    if (cached.symbol.isValid) return cached
                }
            }
        }

        val computed = computeSymbolTypeDeclaration(variable) ?: return null

        cacheLock.write {
            variableCache[cacheKey] = CacheEntry(
                variable.containingFile?.virtualFile?.path ?: return null,
                variable.textRange.startOffset,
                SoftReference(computed)
            )
        }

        return computed
    }

    private fun computeSymbolTypeDeclaration(variable: PsiElement): SymbolTypeDeclaration? {
        val typeDeclaration = getTypeElement(variable) ?: return null
        val virtualFile = variable.containingFile?.virtualFile ?: return null
        val isProjectContent = ProjectFileIndex.getInstance(variable.project).isInContent(virtualFile)
        return SymbolTypeDeclaration(variable as PsiNameIdentifierOwner, CodeNode(typeDeclaration, isProjectContent))
    }

    // 辅助函数，用于判断一个元素是否可能是变量
    private fun isLikelyVariable(element: PsiElement): Boolean {
        val elementClass = element.javaClass.simpleName
        return elementClass.contains("Variable", ignoreCase = true) ||
               elementClass.contains("Parameter", ignoreCase = true) ||
               elementClass.contains("Field", ignoreCase = true)
    }

    // 辅助函数，用于获取变量的类型元素
    private fun getTypeElement(element: PsiElement): PsiElement? {
        return ReadAction.compute<PsiElement?, Throwable> {
            val project = element.project
            val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return@compute null
            val offset = element.textOffset

            GotoTypeDeclarationAction.findSymbolType(editor, offset)
        }
    }

    fun PsiElement.foldTextOfLevel(foldingLevel: Int = 1): String {
        var result: String
        val executionTime = measureTimeMillis {
            val cacheKey = "${containingFile.virtualFile.path}:${textRange.startOffset}:$foldingLevel"

            // 检查缓存
            result = foldCache[cacheKey]?.get()?.let { cachedEntry ->
                val cachedElement = cachedEntry.elementPointer.element
                if (cachedElement != null && cachedElement.isValid &&
                    text.length == cachedEntry.elementLength &&
                    text.hashCode() == cachedEntry.elementHash) {
                    cachedEntry.foldedText
                } else null
            } ?: run {
                // 如果缓存无效或不存在，重新计算
                val foldedText = computeFoldedText(foldingLevel)
                // 更新缓存
                val elementPointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(this)
                foldCache[cacheKey] = SoftReference(FoldCacheEntry(foldedText, elementPointer, text.length, text.hashCode()))
                foldedText
            }
        }

        // 记录执行时间
        Log.info("foldTextOfLevel execution time: $executionTime ms")

        // 返回计算结果
        return result
    }

    private fun PsiElement.computeFoldedText(foldingLevel: Int): String {
        val file = this.containingFile
        val document = file.viewProvider.document ?: return text
        val fileNode = file.node ?: return text

        val foldingBuilder = LanguageFolding.INSTANCE.forLanguage(this.language) ?: return text
        var descriptors: List<FoldingDescriptor> = listOf()
        var timeTaken = measureTimeMillis {
            descriptors = foldingBuilder.buildFoldRegions(fileNode, document)
                .filter {
                    textRange.contains(it.range)
//                        && it.element.textRange.startOffset > textRange.startOffset  // Exclude the function itself
                }
                .sortedBy { it.range.startOffset }
                .let {
                    findDescriptorsOfFoldingLevel(it, foldingLevel)
                }
        }
        Log.info("=============> [$this] Time taken to build fold regions: $timeTaken ms, ${file.virtualFile.path}")
        var result = ""
        timeTaken = measureTimeMillis {
            result = foldTextByDescriptors(descriptors)
        }
        Log.info("=============> [$this] Time taken to fold text: $timeTaken ms, ${file.virtualFile.path}")
        return result
    }

    private fun findDescriptorsOfFoldingLevel(
        descriptors: List<FoldingDescriptor>,
        foldingLevel: Int
    ): List<FoldingDescriptor> {
        val nestedDescriptors = mutableListOf<FoldingDescriptor>()
        val stack = mutableListOf<FoldingDescriptor>()

        for (descriptor in descriptors.sortedBy { it.range.startOffset }) {
            while (stack.isNotEmpty() && !stack.last().range.contains(descriptor.range)) {
                stack.removeAt(stack.size - 1)
            }
            stack.add(descriptor)
            if (stack.size == foldingLevel) {
                nestedDescriptors.add(descriptor)
            }
        }

        return nestedDescriptors
    }

    private fun PsiElement.foldTextByDescriptors(descriptors: List<FoldingDescriptor>): String {
        val sortedDescriptors = descriptors.sortedBy { it.range.startOffset }
        val builder = StringBuilder()
        var currentIndex = 0

        for (descriptor in sortedDescriptors) {
            val range = descriptor.range.shiftRight(-this.textRange.startOffset)
            if (range.startOffset >= currentIndex) {
                builder.append(text, currentIndex, range.startOffset)
                builder.append(descriptor.placeholderText)
                currentIndex = range.endOffset
            }
        }
        builder.append(text.substring(currentIndex))

        return builder.toString()
    }
}