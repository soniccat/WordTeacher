package com.aglushkov.wordteacher.shared.general.article_parser

import org.jsoup.Jsoup
import org.jsoup.internal.StringUtil
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.NodeTraversor
import org.jsoup.select.NodeVisitor
import java.util.Stack

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

    private val tagAsNewLine = setOf(
        "title", "p", "h1", "h2", "h3", "h4", "h5", "h6", "pre", "address", "li", "th", "td"
    )

    private val headerTag = setOf(
        "h1", "h2", "h3", "h4", "h5", "h6"
    )

    sealed interface StyleTag {
        val depth: Int
        val name: String

        data class Header(val size: Int, override val depth: Int): StyleTag {
            companion object {
                fun fromString(str: String, depth: Int) = Header(
                    size = str.substring(1).toIntOrNull() ?: 6,
                    depth = depth
                )
            }

            override val name: String
                get() = "h$size"
        }

        fun startTag(): String = "<$name>"
        fun endTag(): String = "</$name>"
    }

    fun wholeText(element: Element): String? {
        val accum = StringUtil.borrowBuilder()
        var needAddNewLine = false
        var gotTextAfterNewLine = false
        var newLineDepth = -1
        NodeTraversor.traverse(object : NodeVisitor {

            private var styleTags = ArrayDeque<StyleTag>()

            override fun head(node: Node, depth: Int) {
                if (node is Element) {
                    var newStyleTag: StyleTag? = null
                    if (headerTag.contains(node.tag().name)) {
                        newStyleTag = StyleTag.Header.fromString(node.tag().name, depth)
                    }
                    newStyleTag?.let {
                        styleTags.addLast(it)
                        accum.append(it.startTag())
                    }

                    if (!needAddNewLine || tagAsNewLine.contains(node.tag().name)) {
                        needAddNewLine = true
                        gotTextAfterNewLine = false
                        newLineDepth = depth
                    }
                } else if (node is TextNode) {
                    val text = node.wholeText.replace('\n', ' ')
                    if (text.trim().isNotEmpty()) {
                        if (needAddNewLine) {
                            gotTextAfterNewLine = true
                        }
                        accum.append(text)
                    }
                }
            }

            override fun tail(node: Node, depth: Int) {
                if (node !is TextNode) {
                    while (styleTags.lastOrNull()?.depth == depth) {
                        val tag = styleTags.removeLast()
                        accum.append(tag.endTag())
                    }

                    if (gotTextAfterNewLine && depth <= newLineDepth) {
                        accum.append("\n")
                        needAddNewLine = false
                        gotTextAfterNewLine = false
                        newLineDepth = -1
                    }
                }
            }
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