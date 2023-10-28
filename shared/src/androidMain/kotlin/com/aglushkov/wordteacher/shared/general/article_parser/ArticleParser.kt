package com.aglushkov.wordteacher.shared.general.article_parser

import org.jsoup.Jsoup
import org.jsoup.internal.StringUtil
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.NodeTraversor
import org.jsoup.select.NodeVisitor

actual class ArticleParser actual constructor() {
    private var title: String? = null
    private var bodyArticleElement: ArticleElement? = null
    private var resultArticleElement: ArticleElement? = null

    actual fun parse(html: String) : ParsedArticle {
        val doc: Document = Jsoup.parse(html)
        title = doc.title()
        val body = doc.body()
        val bodyArticleElement = ArticleElement(
            element = body,
            childElements = body.children(),
            text = body.ownText(),
        )

        buildNodeTree(bodyArticleElement)
        val articleElement = findArticleElement(bodyArticleElement)

        this.bodyArticleElement = bodyArticleElement
        this.resultArticleElement = articleElement

        return createParsedArticle(title, articleElement)
    }

    actual fun largerArticle(): ParsedArticle {
        return createParsedArticle(title, resultArticleElement!!)
    }

    actual fun smallerArticle(): ParsedArticle {
        return createParsedArticle(title, resultArticleElement!!)
    }

    // cut outer parts until we cut ARTICLE_MAX_CUT_PART of the article
    private fun findArticleElement(bodyArticleElement: ArticleElement): ArticleElement {
        val textLength = bodyArticleElement.textLenWithChildren
        var articleElement = bodyArticleElement
        var cutPart = 0.0

        while (true) {
            val noteTextPart = articleElement.textLength() / textLength.toFloat()

            if (cutPart + noteTextPart >= ARTICLE_MAX_CUT_PART) {
                break;
            } else {
                cutPart += noteTextPart
            }

            if (articleElement.childArticleElements.size == 1) {
                articleElement = articleElement.childArticleElements.first()

            } else {
                val childTextParts = articleElement.childArticleElements.mapIndexed { i, e ->
                    i to e.textLenWithChildren / textLength.toFloat()
                }.sortedBy { it.second }

                val cutPartWithoutLast = childTextParts.take(childTextParts.size - 1).map {
                    it.second
                }.sum()

                if (cutPart + cutPartWithoutLast >= ARTICLE_MAX_CUT_PART) {
                    break
                } else {
                    cutPart += cutPartWithoutLast
                    articleElement =
                        articleElement.childArticleElements[childTextParts.last().first]
                }
            }
        }
        return articleElement
    }

    private fun createParsedArticle(
        title: String?,
        articleElement: ArticleElement
    ) = ParsedArticle(
        title,
        wholeText(articleElement.element)
    )

    fun wholeText(element: Element): String? {
        val accum = StringUtil.borrowBuilder()
        NodeTraversor.traverse(object : NodeVisitor {
            override fun head(node: Node, depth: Int) {
                if (node is TextNode) {
                    accum.append(node.wholeText + "\n")
                }
            }

            override fun tail(node: Node, depth: Int) {}
        }, element)
        return StringUtil.releaseBuilder(accum)
    }

    private fun buildNodeTree(rootElement: ArticleElement) {
        val articleElements = rootElement.childElements.map {
            ArticleElement(
                element = it,
                text = it.ownText(),
                childElements = it.children()
            )
        }
        rootElement.childArticleElements = articleElements

        articleElements.onEach {
            buildNodeTree(it)
        }

        val noteTextLen = rootElement.textLength()
        if (rootElement.childElements.isEmpty()) {
            rootElement.textLenWithChildren = noteTextLen
        } else {
            rootElement.textLenWithChildren = rootElement.childArticleElements.map {
                it.textLenWithChildren
            }.sum() + noteTextLen
        }
    }
}

data class ArticleElement(
    val element: Element,
    val childElements: List<Element>,
    val text: String?,
    var childArticleElements: List<ArticleElement> = emptyList(),
    var textLenWithChildren: Int = 0,
) {
    fun textLength() = text?.length ?: 0
}

private const val ARTICLE_MAX_CUT_PART = 0.5