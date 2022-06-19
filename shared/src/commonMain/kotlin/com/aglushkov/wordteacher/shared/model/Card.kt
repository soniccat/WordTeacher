package com.aglushkov.wordteacher.shared.model

import com.aglushkov.wordteacher.shared.general.TimeSource

data class Card (
    val id: Long,
    val date: Long,
    val term: String,
    val definitions: List<String>,
    val definitionTermSpans: List<List<Pair<Int, Int>>>,
    val partOfSpeech: WordTeacherWord.PartOfSpeech,
    val transcription: String?,
    val synonyms: List<String>,
    val examples: List<String>,
    val progress: CardProgress,
) {
    fun withRightAnswer(timeSource: TimeSource) =
        copy(
            progress = progress.withRightAnswer(timeSource)
        )

    fun withWrongAnswer(timeSource: TimeSource) =
        copy(
            progress = progress.withWrongAnswer(timeSource)
        )
}
