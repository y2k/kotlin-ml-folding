package io.y2k.kotlinml

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil

class KotlinMLFolding : FoldingBuilderEx() {

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> =
        listOf(")", "}")
            .flatMap { mkBraceFoldings(root, it) }
            .toTypedArray()

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
