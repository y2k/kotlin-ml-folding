package io.y2k.kotlinml

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.annotations.NotNull

class KotlinMLFolding : FoldingBuilderEx() {

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> =
        PsiTreeUtil
            .findChildrenOfType(root, LeafPsiElement::class.java)
            .map {
                object {
                    val item = it
                    val prev = it.prevSibling
                }
            }
            .filter { it.item.text == "}" && it.prev.text?.startsWith("\n") ?: false }
            .map {
                FoldingDescriptor(
                    it.item.node,
                    TextRange(it.prev.textRange.startOffset, it.item.textRange.endOffset))
            }
            .toTypedArray()

    override fun getPlaceholderText(@NotNull node: ASTNode): String = " }"

    override fun isCollapsedByDefault(@NotNull node: ASTNode): Boolean = true
}
