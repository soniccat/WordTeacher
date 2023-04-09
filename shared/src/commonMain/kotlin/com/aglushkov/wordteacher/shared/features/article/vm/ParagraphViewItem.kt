package com.aglushkov.wordteacher.shared.features.article.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentence
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ParagraphViewItem(
    override val id: Long,
    override val items: ImmutableList<NLPSentence>,
    override val type: Int = 201,
    val annotations: List<List<ArticleAnnotation>>
): BaseViewItem<NLPSentence>{
    override fun copyWithId(id: Long): BaseViewItem<NLPSentence> = copy(id = id)
}
