package com.aglushkov.wordteacher.shared.general.article_parser


actual class ArticleParser actual constructor() {
    actual fun parse(html: String) : ParsedArticle {
        return ParsedArticle("", "")
    }

    actual fun largerArticle(): ParsedArticle {
        return ParsedArticle("", "")
    }

    actual fun smallerArticle(): ParsedArticle {
        return ParsedArticle("", "")
    }
}
