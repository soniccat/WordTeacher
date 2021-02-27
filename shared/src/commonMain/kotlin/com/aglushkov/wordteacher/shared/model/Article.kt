package com.aglushkov.wordteacher.shared.model

import com.aglushkov.wordteacher.shared.model.nlp.NLPSentence

class Article(
    id: Long,
    name: String,
    date: Long,
    val text: String,
    var sentences: List<NLPSentence> = emptyList()
): ShortArticle(id, name, date)
