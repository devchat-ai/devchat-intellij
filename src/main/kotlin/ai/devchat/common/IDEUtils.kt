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

object IDEUtils {
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

        return sequence {
            // 从当前元素开始，向上遍历所有父元素
            var currentScope: PsiElement? = this@findAccessibleVariables
            while (currentScope != null && currentScope !is PsiFile) {
                // 在当前作用域中搜索变量声明
                val variablesInScope = PsiTreeUtil.findChildrenOfAnyType(
                    currentScope,
                    false,
                    PsiNameIdentifierOwner::class.java
                )

                for (variable in variablesInScope) {
                    if (isLikelyVariable(variable) && !variable.name.isNullOrEmpty() && variable.nameIdentifier != null) {
                        val typeDeclaration = getTypeElement(variable)
                        if (typeDeclaration != null) {
                            val virtualFile = variable.containingFile?.virtualFile
                            if (virtualFile != null) {
                                val isProjectContent = projectFileIndex.isInContent(virtualFile)
                                yield(SymbolTypeDeclaration(variable, CodeNode(typeDeclaration, isProjectContent)))
                            }
                        }
                    }
                }

                // 移动到父作用域
                currentScope = currentScope.parent
            }

            // 最后检查文件级别的变量
            yieldAll(this@findAccessibleVariables.containingFile.children
                .asSequence()
                .filterIsInstance<PsiNameIdentifierOwner>()
                .filter { isLikelyVariable(it) && !it.name.isNullOrEmpty() && it.nameIdentifier != null }
                .mapNotNull { variable ->
                    val typeElement = getTypeElement(variable) ?: return@mapNotNull null
                    val virtualFile = variable.containingFile?.virtualFile ?: return@mapNotNull null
                    val isProjectContent = projectFileIndex.isInContent(virtualFile)
                    SymbolTypeDeclaration(variable, CodeNode(typeElement, isProjectContent))
                })
        }
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
        val project = element.project
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return null
        val offset = element.textOffset

        return GotoTypeDeclarationAction.findSymbolType(editor, offset)
    }

    fun PsiElement.foldTextOfLevel(foldingLevel: Int = 1): String {
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
            val range = descriptor.range.shiftRight(-startOffset)
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