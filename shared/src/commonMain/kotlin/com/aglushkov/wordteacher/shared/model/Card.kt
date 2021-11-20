package com.aglushkov.wordteacher.shared.model

data class Card (
    var id: Long,
    var date: Long,
    var term: String,
    var definitions: List<String>,
    var partOfSpeech: WordTeacherWord.PartOfSpeech,
    var transcription: String?,
    var synonyms: List<String>,
    var examples: List<String>
) {

}
