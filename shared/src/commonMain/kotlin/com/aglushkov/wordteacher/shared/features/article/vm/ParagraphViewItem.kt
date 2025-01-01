package com.aglushkov.wordteacher.shared.features.article.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.model.ArticleStyle
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentence

class ParagraphViewItem(
    paragraphId: Long,
    sentences: List<NLPSentence>,
    val annotations: List<List<ArticleAnnotation>>,
    val styles: ArticleStyle,
): BaseViewItem<NLPSentence>(sentences, Type, paragraphId) {
    companion object {
        const val Type = 201
    }
}
