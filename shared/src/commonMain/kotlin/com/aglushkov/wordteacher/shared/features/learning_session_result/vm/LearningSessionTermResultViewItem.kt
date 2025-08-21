package com.aglushkov.wordteacher.shared.features.learning_session_result.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.model.WordTeacherWord

class LearningSessionTermResultViewItem(
    val cardId: Long,
    val term: String,
    val partOfSpeech: WordTeacherWord.PartOfSpeech,
    var newProgress: Float,
    var isRight: Boolean,
): BaseViewItem<String>("", Type, cardId) {
    companion object {
        const val Type = 901
    }
}
