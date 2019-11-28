package io.y2k.kotlinml

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.psiUtil.nextLeafs

class KotlinMLFolding : FoldingBuilderEx() {

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean) =
        listOf(
            mkBraceFoldings(root, ")"),
            mkBraceFoldings(root, "}"),
            mkBraceFoldingsForElse(root),
            mkLetFoldings(root)
        ).flatten().toTypedArray()

    private fun mkBraceFoldings(root: PsiElement, braceText: String) =
        PsiTreeUtil
            .findChildrenOfType(root, LeafPsiElement::class.java)
            .map {
                object {
                    val item = it
                    val prev = it.prevSibling
                }
            }
            .filter {
                it.item.text == braceText
                    && it.prev?.text?.startsWith("\n") ?: false
                    && it.item.nextLeafs.firstOrNull()?.text?.startsWith("\n") ?: false
            }
            .map {
                FoldingDescriptor(
                    it.item.node,
                    TextRange(it.prev.textRange.startOffset, it.item.textRange.endOffset),
                    null,
                    " $braceText"
                )
            }

    private fun mkBraceFoldingsForElse(root: PsiElement) = run {
        fun filter(item: LeafPsiElement) = run {
            val actual = item.nextLeafs.take(2).toList()
            val expected = listOf<(PsiElement) -> Boolean>(
                { it is PsiWhiteSpace },
                { it is LeafPsiElement && (it.elementType as? KtKeywordToken)?.value == "else" }
            )

            val count = actual.zip(expected) { e, f -> f(e) }.count { it }
            count == expected.size
        }

        val brace = "}"
        PsiTreeUtil
            .findChildrenOfType(root, LeafPsiElement::class.java)
            .map {
                object {
                    val item = it
                    val prev = it.prevSibling
                }
            }
            .filter {
                it.item.text == brace
                    && it.prev?.text?.startsWith("\n") ?: false
                    && filter(it.item)
            }
            .map {
                FoldingDescriptor(
                    it.item.node,
                    TextRange(it.item.textRange.startOffset - 2, it.item.textRange.endOffset),
                    null,
                    brace
                )
            }
    }

    private fun mkLetFoldings(root: PsiElement) =
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
                        val end = FoldingDescriptor(
                            it.node.node,
                            TextRange(l.textRange.startOffset, l.textRange.endOffset),
                            group,
                            ""
                        )

                        listOf(start, end)
                    }
                    it.node.text.startsWith("let {") -> listOf(mkPipeFolding(it.node, it.dot))
                    else -> emptyList()
                }
            }

    private fun mkPipeFolding(node: KtCallExpression, dot: PsiElement, group: FoldingGroup? = null): FoldingDescriptor =
        FoldingDescriptor(
            node.node,
            TextRange(dot.textRange.startOffset - 4, dot.textRange.startOffset + 5),
            group,
            "|> "
        )

    override fun isCollapsedByDefault(node: ASTNode): Boolean = true
    override fun getPlaceholderText(node: ASTNode): String? = null
}
