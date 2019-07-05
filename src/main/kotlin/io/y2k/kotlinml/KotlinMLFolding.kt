package io.y2k.kotlinml

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtCallExpression

class KotlinMLFolding : FoldingBuilderEx() {

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> =
        listOf(")", "}")
            .flatMap { mkBraceFoldings(root, it) }
            .plus(mkLetFoldings(root))
            .toTypedArray()

    private fun mkLetFoldings(root: PsiElement): List<FoldingDescriptor> =
        PsiTreeUtil
            .findChildrenOfType(root, KtCallExpression::class.java)
            .map {
                object {
                    val dot = it.prevSibling
                    val node = it
                }
            }
            .filter { it.dot?.text == "." && it.node.text.startsWith("let") }
            .flatMap {
                when {
                    it.node.text.startsWith("let(") -> {
                        val group = FoldingGroup.newGroup("pipe")

                        val start = mkPipeFolding(it.node, it.dot, group)

                        val l = it.node.lastChild.lastChild
                        val end = object : FoldingDescriptor(
                            it.node.node,
                            TextRange(l.textRange.startOffset, l.textRange.endOffset),
                            group) {
                            override fun getPlaceholderText() = ""
                        }

                        listOf(start, end)
                    }
                    it.node.text.startsWith("let {") -> listOf(mkPipeFolding(it.node, it.dot))
                    else -> emptyList()
                }
            }

    private fun mkPipeFolding(node: KtCallExpression, dot: PsiElement, group: FoldingGroup? = null): FoldingDescriptor =
        object : FoldingDescriptor(
            node.node,
            TextRange(dot.textRange.startOffset - 4, dot.textRange.startOffset + 5),
            group) {
            override fun getPlaceholderText() = "|> "
        }

    private fun mkBraceFoldings(root: PsiElement, braceText: String): List<FoldingDescriptor> =
        PsiTreeUtil
            .findChildrenOfType(root, LeafPsiElement::class.java)
            .map {
                object {
                    val item = it
                    val prev = it.prevSibling
                }
            }
            .filter { it.item.text == braceText && it.prev?.text?.startsWith("\n") ?: false }
            .map {
                object : FoldingDescriptor(
                    it.item.node,
                    TextRange(it.prev.textRange.startOffset, it.item.textRange.endOffset)) {
                    override fun getPlaceholderText() = " $braceText"
                }
            }

    override fun isCollapsedByDefault(node: ASTNode): Boolean = true
    override fun getPlaceholderText(node: ASTNode): String? = null
}
