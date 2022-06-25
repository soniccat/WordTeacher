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
    val exampleTermSpans: List<List<Pair<Int, Int>>>,
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

    fun resolveDefinitionsWithHiddenTerm(): List<String> =
        resolveWithHiddenTerm(definitions, definitionTermSpans)

    fun resolveExamplesWithHiddenTerm(): List<String> =
        resolveWithHiddenTerm(examples, exampleTermSpans)

    fun resolveWithHiddenTerm(strings: List<String>, spans: List<List<Pair<Int, Int>>>): List<String> =
        strings.mapIndexed { i, str ->
            spans.getOrNull(i)?.let { spans ->
                var resString: CharSequence = str
                spans.asReversed().forEach { span ->
                    resString = resString.replaceRange(span.first, span.second, TERM_REPLACEMENT)
                }
                resString.toString()
            } ?: str
        }
}

private const val TERM_REPLACEMENT = "___"