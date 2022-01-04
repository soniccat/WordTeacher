package com.aglushkov.wordteacher.shared.features.learning_session_result.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem

class LearningSessionTermResultViewItem(
    val cardId: Long,
    val term: String,
    var newProgress: Float,
    var isRight: Boolean,
): BaseViewItem<String>("", Type, cardId) {
    companion object {
        const val Type = 901
    }
}
