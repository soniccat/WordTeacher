package com.aglushkov.wordteacher.shared.model

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.repository.db.UNDEFINED_FREQUENCY
import com.aglushkov.wordteacher.shared.repository.db.UNKNOWN_FREQUENCY
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Card (
    @Transient val id: Long = 0,
    @SerialName("id") val remoteId: String,
    val creationDate: Instant,
    val modificationDate: Instant,
    val term: String,
    val definitions: List<String>,
    val labels: List<String> = emptyList(),
    val definitionTermSpans: List<List<CardSpan>>,
    val partOfSpeech: WordTeacherWord.PartOfSpeech,
    val transcriptions: List<String>,
    val audioFiles: List<WordTeacherWord.AudioFile>,
    val synonyms: List<String>,
    val examples: List<String>,
    val exampleTermSpans: List<List<CardSpan>>,
    val progress: CardProgress,
    val needToUpdateDefinitionSpans: Boolean,
    val needToUpdateExampleSpans: Boolean,
    val creationId: String,
    @Transient val termFrequency: Double = UNDEFINED_FREQUENCY,
) {
    fun withRightAnswer(timeSource: TimeSource) =
        copy(
            progress = progress.withRightAnswer(timeSource),
        )

    fun withWrongAnswer(timeSource: TimeSource) =
        copy(
            progress = progress.withWrongAnswer(timeSource),
        )

    fun resolveDefinitionsWithHiddenTerm(): List<String> =
        resolveStringsWithHiddenSpans(definitions, definitionTermSpans)

    fun resolveExamplesWithHiddenTerm(): List<String> =
        resolveStringsWithHiddenSpans(examples, exampleTermSpans)
}

fun resolveStringsWithHiddenSpans(strings: List<String>, spans: List<List<CardSpan>>): List<String> =
    strings.mapIndexed { i, str ->
        spans.getOrNull(i)?.let { spans ->
            resolveStringWithHiddenSpans(str, spans)
        } ?: str
    }

fun resolveStringWithHiddenSpans(str: String, spans: List<CardSpan>): String {
    var resString: CharSequence = str
    spans.asReversed().forEach { span ->
        if (span.end <= resString.length) {
            resString = resString.replaceRange(span.start, span.end, TERM_REPLACEMENT)
        } else {
            Logger.e("$resString is shorter than expected span ${span.start}:${span.end}")
        }
    }
    return resString.toString()
}


// here we don't merge the content of two cards, we just choose the newest one
fun List<Card>.mergeCards(anotherCards: List<Card>): List<Card> {
    val anotherCardsMap = anotherCards.associateBy { it.creationId }.toMutableMap()
    return buildList<Card> {
        this@mergeCards.onEach { thisCard ->
            anotherCardsMap[thisCard.creationId]?.let { remoteCard ->
                if (thisCard.modificationDate > remoteCard.modificationDate) {
                    add(thisCard)
                } else {
                    add(remoteCard)
                }
            } ?: run {
                add(thisCard)
            }
            anotherCardsMap.remove(thisCard.creationId)
        }

        anotherCardsMap.onEach {
            add(it.value)
        }
    }
}

@Serializable
data class CardSpan(
    val start: Int,
    val end: Int
)

private const val TERM_REPLACEMENT = "＿"
