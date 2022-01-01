package com.aglushkov.wordteacher.shared.model

data class MutableCard (
    override var id: Long,
    override var date: Long,
    override var term: String,
    override var definitions: MutableList<String>,
    override var partOfSpeech: WordTeacherWord.PartOfSpeech,
    override var transcription: String?,
    override var synonyms: MutableList<String>,
    override var examples: MutableList<String>,
    override val progress: MutableCardProgress,
) : Card {
    fun set(card: Card) {
        id = card.id
        date = card.date
        term = card.term
        definitions = card.definitions.toMutableList()
        partOfSpeech = card.partOfSpeech
        transcription = card.transcription
        synonyms = card.synonyms.toMutableList()
        examples = card.examples.toMutableList()
        progress.set(card.progress)
    }
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
    override val progress: ImmutableCardProgress,
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

    fun toMutableCard() =
        MutableCard(
            id = id,
            date = date,
            term = term,
            definitions = definitions.toMutableList(),
            partOfSpeech = partOfSpeech,
            transcription = transcription,
            synonyms = synonyms.toMutableList(),
            examples = examples.toMutableList(),
            progress = progress.toMutableCardProgress()
        )

    fun toImmutableCard() =
        if (this is ImmutableCard) {
            this
        } else {
            ImmutableCard(
                id = id,
                date = date,
                term = term,
                definitions = definitions.toMutableList(),
                partOfSpeech = partOfSpeech,
                transcription = transcription,
                synonyms = synonyms.toMutableList(),
                examples = examples.toMutableList(),
                progress = progress.toImmutableCardProgress()
            )
        }
}
