package com.aglushkov.wordteacher.shared.general.article_parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

actual class ArticleParser actual constructor() {
    actual fun parse(html: String) : ParsedArticle {
        val doc: Document = Jsoup.parse(html)
        val body = doc.body()
        val bodyNode = ArticleNode(
            node = body,
            childNodes = body.childNodes(),
            text = body.ownText(),
        )

        buildNodeTree(bodyNode)

        val textLength = bodyNode.textLenWithChildren
        var articleNode = bodyNode

        val maxCutPart = 0.5
        var cutPart = 0.0

        while (true) {
            val noteTextPart = articleNode.textLength() / textLength.toFloat()

            if (cutPart + noteTextPart >= maxCutPart) {
                break;
            } else {
                cutPart += noteTextPart
            }

            if (articleNode.childArticleNodes.size == 1) {
                articleNode = articleNode.childArticleNodes.first()
            } else {

                val childTextParts = articleNode.childArticleNodes.mapIndexed { i, e ->
                    i to e.textLenWithChildren / textLength.toFloat()
                }.sortedBy { it.second }

                val cutPartWithoutLast = childTextParts.take(childTextParts.size - 1).map {
                    it.second
                }.sum()

                if (cutPart + cutPartWithoutLast >= maxCutPart) {
                    break
                } else {
                    cutPart += cutPartWithoutLast
                    articleNode = articleNode.childArticleNodes[childTextParts.last().first]
                }
            }
        }

        return ParsedArticle(
            doc.title(),
            when (val node = articleNode.node) {
                is TextNode -> node.wholeText
                is Element -> node.wholeText()
                else -> ""
            }
        )
    }

    private fun buildNodeTree(rootNode: ArticleNode) {
        val articleNodes = rootNode.childNodes.map {
            ArticleNode(
                node = it,
                text = when (it) {
                    is TextNode -> it.wholeText
                    is Element -> it.ownText()
                    else -> ""
                },
                childNodes = it.childNodes()
            )
        }
        rootNode.childArticleNodes = articleNodes

        articleNodes.onEach {
            buildNodeTree(it)
        }

        val noteTextLen = rootNode.textLength()
        if (rootNode.childNodes.isEmpty()) {
            rootNode.textLenWithChildren = noteTextLen
        } else {
            rootNode.textLenWithChildren = rootNode.childArticleNodes.map {
                it.textLenWithChildren
            }.sum() + noteTextLen
        }
    }
}

data class ArticleNode(
    val node: Node,
    val childNodes: List<Node>,
    val text: String?,
    var childArticleNodes: List<ArticleNode> = emptyList(),
    var textLenWithChildren: Int = 0,
) {
    fun textLength() = text?.length ?: 0
}
