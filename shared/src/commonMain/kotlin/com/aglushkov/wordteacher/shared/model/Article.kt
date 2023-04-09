package com.aglushkov.wordteacher.shared.model

import com.aglushkov.wordteacher.shared.model.nlp.NLPSentence
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

class Article(
    id: Long,
    name: String,
    date: Long,
    var sentences: ImmutableList<NLPSentence> = persistentListOf(),
    var style: ArticleStyle = ArticleStyle()
): ShortArticle(id, name, date)
