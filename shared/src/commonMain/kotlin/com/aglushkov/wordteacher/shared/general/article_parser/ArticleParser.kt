package com.aglushkov.wordteacher.shared.general.article_parser

expect class ArticleParser() {
    fun parse(html: String) : ParsedArticle
    fun largerArticle(): ParsedArticle
    fun smallerArticle(): ParsedArticle
}

data class ParsedArticle(
    val title: String?,
    val text: String?
)
