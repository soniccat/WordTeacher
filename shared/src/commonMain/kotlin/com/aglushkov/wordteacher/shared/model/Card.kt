package com.aglushkov.wordteacher.shared.model

data class Card (
    var id: Long,
    var date: Long,
    var term: String,
    var definitions: MutableList<String>,
    var partOfSpeech: WordTeacherWord.PartOfSpeech,
    var transcription: String?,
    var synonyms: MutableList<String>,
    var examples: MutableList<String>
) {

}
