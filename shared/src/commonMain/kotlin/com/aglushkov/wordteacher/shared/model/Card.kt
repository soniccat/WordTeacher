package com.aglushkov.wordteacher.shared.model

data class Card (
    val id: Long,
    val date: Long,
    val term: String,
    val definitions: List<String>,
    val partOfSpeech: WordTeacherWord.PartOfSpeech,
    val transcription: String?,
    val synonyms: List<String>,
    val examples: List<String>
) {

}