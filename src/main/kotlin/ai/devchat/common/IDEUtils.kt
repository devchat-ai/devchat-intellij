package ai.devchat.common

import com.intellij.lang.folding.LanguageFolding
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.util.elementType
import com.intellij.psi.util.findParentInFile
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch


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
                            when (ref) {
                                is PsiPolyVariantReference -> ref.multiResolve(false).mapNotNull { it.element }
                                else -> listOfNotNull(ref.resolve())
                            }.filter { resolved ->
                                resolved.containingFile.virtualFile?.let { file ->
                                    projectFileIndex.isInContent(file)
                                } == true
                            }
                        }
                    }
                    .firstOrNull { it.isNotEmpty() }
            }
    }

    fun PsiElement.getFoldedText(): String {
        val file = this.containingFile
        val document = file.viewProvider.document ?: return text

        val foldingBuilder = LanguageFolding.INSTANCE.forLanguage(this.language) ?: return text
        val descriptors = foldingBuilder.buildFoldRegions(file.node, document)

        // Find the largest folding descriptor that is contained within the element's range
        val bodyDescriptor = descriptors
            .filter {
                textRange.contains(it.range)
                        && it.element.textRange.startOffset > textRange.startOffset  // Exclude the function itself
            }
            .sortedByDescending { it.range.length }
            .getOrNull(0)
            ?: return text

        val bodyStart = bodyDescriptor.range.startOffset - textRange.startOffset
        val bodyEnd = bodyDescriptor.range.endOffset - textRange.startOffset

        return buildString {
            append(text.substring(0, bodyStart))
            append(bodyDescriptor.placeholderText)
            append(text.substring(bodyEnd))
        }
    }
}