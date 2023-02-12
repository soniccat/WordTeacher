package com.aglushkov.wordteacher.shared.model

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.extensions.merge
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Card (
    val id: Long,
    val remoteId: String,
    val creationDate: Instant,
    val modificationDate: Instant,
    val term: String,
    val definitions: List<String>,
    val definitionTermSpans: List<List<Pair<Int, Int>>>,
    val partOfSpeech: WordTeacherWord.PartOfSpeech,
    val transcription: String?,
    val synonyms: List<String>,
    val examples: List<String>,
    val exampleTermSpans: List<List<Pair<Int, Int>>>,
    val progress: CardProgress,
    val needToUpdateDefinitionSpans: Boolean,
    val needToUpdateExampleSpans: Boolean,
    val creationId: String,
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
                    if (span.second <= resString.length) {
                        resString = resString.replaceRange(span.first, span.second, TERM_REPLACEMENT)
                    } else {
                        Logger.e("$resString is shorter than expected span ${span.first}:${span.second}")
                    }
                }
                resString.toString()
            } ?: str
        }
}

// here we don't merge the content of two cards, we just choose the newest one
fun List<Card>.mergeCards(anotherCards: List<Card>): List<Card> {
    val anotherCardsMap = anotherCards.associateBy { it.remoteId }.toMutableMap()
    return buildList<Card> {
        this.onEach { thisCard ->
            anotherCardsMap[thisCard.remoteId]?.let { remoteCard ->
                if (thisCard.modificationDate > remoteCard.modificationDate) {
                    add(thisCard)
                } else {
                    add(remoteCard)
                }
            } ?: run {
                add(thisCard)
            }
            anotherCardsMap.remove(thisCard.remoteId)
        }

        anotherCardsMap.onEach {
            add(it.value)
        }
    }
}

private const val TERM_REPLACEMENT = "___"
