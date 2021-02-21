package com.aglushkov.wordteacher.shared.model

import com.aglushkov.wordteacher.shared.model.nlp.NLPSentence

class Article(
    var id: Long,
    val name: String,
    val date: Long,
    val text: String,
    var sentences: List<NLPSentence> = emptyList()
)
