package com.aglushkov.wordteacher.shared.model

import com.aglushkov.wordteacher.shared.features.learning.cardteacher.CardProgress

data class MutableCard (
    override var id: Long,
    override var date: Long,
    override var term: String,
    override var definitions: MutableList<String>,
    override var partOfSpeech: WordTeacherWord.PartOfSpeech,
    override var transcription: String?,
    override var synonyms: MutableList<String>,
    override var examples: MutableList<String>,
    override val progress: CardProgress,
) : Card {

}

data class ImmutableCard (
    override val id: Long,
    override val date: Long,
    override val term: String,
    override val definitions: List<String>,
    override val partOfSpeech: WordTeacherWord.PartOfSpeech,
    override val transcription: String?,
    override val synonyms: List<String>,
    override val examples: List<String>,
    override val progress: CardProgress,
) : Card {

}

interface Card {
    val id: Long
    val date: Long
    val term: String
    val definitions: List<String>
    val partOfSpeech: WordTeacherWord.PartOfSpeech
    val transcription: String?
    val synonyms: List<String>
    val examples: List<String>
    val progress: CardProgress
}
