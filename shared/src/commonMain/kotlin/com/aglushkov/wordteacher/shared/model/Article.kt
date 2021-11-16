package com.aglushkov.wordteacher.shared.model

import com.aglushkov.wordteacher.shared.model.nlp.NLPSentence

class Article(
    id: Long,
    name: String,
    date: Long,
    var sentences: List<NLPSentence> = emptyList(),
    var style: ArticleStyle = ArticleStyle()
): ShortArticle(id, name, date)
