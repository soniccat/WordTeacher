package com.aglushkov.wordteacher.shared.features.learning.vm

import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.model.resolveStringWithHiddenSpans
import com.aglushkov.wordteacher.shared.model.resolveStringsWithHiddenSpans
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

class MatchSession(
    cards: List<Card>,
) {
    companion object {
        fun isValid(cards: List<Card>): Boolean =
            cards.all { it.examples.isNotEmpty() }
    }

    private val matchPairStateFlow = MutableStateFlow<List<MatchPair>?>(null)
    val matchPairFlow: Flow<List<MatchPair>?> = matchPairStateFlow

    private var currentSelectionGroup: Int = 0
    private var lastSelectedTermIndex: Int = -1
    private var lastSelectedExampleIndex: Int = -1

    init {
        val examples = cards.map { card ->
            val exampleIndex = Random.nextInt(card.examples.size)
            val exampleWithGaps = resolveStringWithHiddenSpans(card.examples[exampleIndex], card.exampleTermSpans[exampleIndex])
            exampleWithGaps
        }
        val shuffledExampleIndices = examples.indices.shuffled().toMutableList()

        matchPairStateFlow.update {
            cards.mapIndexed { index, card ->
                MatchPair(card.term, examples[shuffledExampleIndices[index]], shuffledExampleIndices.indexOf(index))
            }
        }
    }

    fun selectTerm(pair: MatchPair) {
        val index = matchPairStateFlow.value?.indexOf(pair) ?: return
        if (index == -1) {
            return
        }

        if (!pair.termSelection.isSelected) {
            if (lastSelectedTermIndex != -1) {
                // remove previous selection
                updatePairs { i, p ->
                    if (i == lastSelectedTermIndex) {
                        p.copy(termSelection = EmptyMatchSelection)
                    } else {
                        p
                    }
                }
            }

            lastSelectedTermIndex = index
        } else {
            if (lastSelectedTermIndex == index) {
                lastSelectedTermIndex = -1
            }
        }

        // update termSelection
        var updatedPair: MatchPair? = null
        updatePairs { i, p ->
            if (i == index) {
                if (!pair.termSelection.isSelected) {
                    pair.copy(
                            termSelection = MatchSelection(
                            oppositeSelectedIndex = lastSelectedExampleIndex,
                            group = currentSelectionGroup,
                        ),
                    ).also {
                        if (it.termSelection.hasMatch()) {
                            ++currentSelectionGroup
                            lastSelectedTermIndex = -1
                            lastSelectedExampleIndex = -1
                        }
                    }
                } else {
                    pair.copy(termSelection = EmptyMatchSelection)
                }.also {
                    updatedPair = it
                }
            } else {
                p
            }
        }

        // update opposite exampleSelection
        updatePairs { i, p ->
            // after deselecting clear corresponding example selection
            if (pair.termSelection.isSelected && pair.termSelection.oppositeSelectedIndex == i) {
                p.copy(exampleSelection = EmptyMatchSelection)
            // after selection update corresponding example oppositeIndex
            } else if (updatedPair?.termSelection?.oppositeSelectedIndex == i) {
                p.copy(exampleSelection = p.exampleSelection.copy(oppositeSelectedIndex = index))
            } else {
                p
            }
        }

        makeCheckIfNeeded()
    }

    fun selectExample(pair: MatchPair) {
        val index = matchPairStateFlow.value?.indexOf(pair) ?: return
        if (index == -1) {
            return
        }

        if (!pair.exampleSelection.isSelected) {
            if (lastSelectedExampleIndex != -1) {
                // remove previous selection
                updatePairs { i, p ->
                    if (i == lastSelectedExampleIndex) {
                        p.copy(exampleSelection = EmptyMatchSelection)
                    } else {
                        p
                    }
                }
            }

            lastSelectedExampleIndex = index
        } else if (lastSelectedExampleIndex == index) {
            lastSelectedExampleIndex = -1
        }

        // update example selection
        var updatedPair: MatchPair? = null
        updatePairs { i, p ->
            if (i == index) {
                if (!pair.exampleSelection.isSelected) {
                    pair.copy(
                            exampleSelection = MatchSelection(
                            oppositeSelectedIndex = lastSelectedTermIndex,
                            group = currentSelectionGroup,
                        ),
                    ).also {
                        if (it.exampleSelection.hasMatch()) {
                            ++currentSelectionGroup
                            lastSelectedTermIndex = -1
                            lastSelectedExampleIndex = -1
                        }
                    }
                } else {
                    pair.copy(exampleSelection = EmptyMatchSelection)
                }.also {
                    updatedPair = it
                }
            } else {
                p
            }
        }

        // update opposite termSelection
        updatePairs { i, p ->
            // after deselecting clear corresponding term selection
            if (pair.exampleSelection.isSelected && pair.exampleSelection.oppositeSelectedIndex == i) {
                p.copy(termSelection = EmptyMatchSelection)
            // after selection update corresponding term oppositeIndex
            } else if (updatedPair?.exampleSelection?.oppositeSelectedIndex == i) {
                p.copy(termSelection = p.termSelection.copy(oppositeSelectedIndex = index))
            } else {
                p
            }
        }

        makeCheckIfNeeded()
    }

    private fun makeCheckIfNeeded() {
        if (matchPairStateFlow.value?.all { it.termSelection.hasMatch() } == false) {
            return
        }

        // update termSelection
        val rightExampleIndexes = mutableSetOf<Int>()
        updatePairs { i, pair ->
            if (pair.rightExampleIndex == pair.termSelection.oppositeSelectedIndex) {
                rightExampleIndexes.add(pair.termSelection.oppositeSelectedIndex)
                pair.copy(termSelection = pair.termSelection.copy(isChecked = true))
            } else {
                pair.copy(termSelection = EmptyMatchSelection)
            }
        }

        // update exampleSelection
        updatePairs { i, pair ->
            if (rightExampleIndexes.contains(i)) {
                pair.copy(exampleSelection = pair.exampleSelection.copy(isChecked = true))
            } else {
                pair.copy(exampleSelection = EmptyMatchSelection)
            }
        }

        if (rightExampleIndexes.size == matchPairStateFlow.value?.size) {
            matchPairStateFlow.update { null }
        }
    }

    private fun updatePairs(transform: (i: Int, pair: MatchPair) -> MatchPair) {
        matchPairStateFlow.update { it?.mapIndexed(transform) }
    }

    @Parcelize
    data class State(
        val cardIds: List<Long>,
    ): Parcelable

    data class MatchPair(
        val term: String,
        val example: String,
        val rightExampleIndex: Int,
        var termSelection: MatchSelection = EmptyMatchSelection,
        var exampleSelection: MatchSelection = EmptyMatchSelection,
    ) {
        override fun hashCode(): Int {
            var result = term.hashCode()
            result = 31 * result + example.hashCode()
            result = 31 * result + rightExampleIndex
            result = 31 * result + termSelection.hashCode()
            result = 31 * result + exampleSelection.hashCode()
            return result
        }

        override fun equals(other: Any?): Boolean {
            val otherMatchPair = other as? MatchPair ?: return false
            return term == otherMatchPair.term &&
                    example == otherMatchPair.example &&
                    rightExampleIndex == other.rightExampleIndex &&
                    termSelection == other.termSelection &&
                    exampleSelection == other.exampleSelection
        }
    }

    data class MatchSelection(
        val oppositeSelectedIndex: Int = -1,
        val group: Int = -1,
        val isChecked: Boolean = false,
    ) {
        val isSelected: Boolean
            get() = oppositeSelectedIndex != -1

        fun hasMatch(): Boolean {
            return oppositeSelectedIndex != -1
        }
    }
}

val EmptyMatchSelection = MatchSession.MatchSelection()

const val MATCH_SESSION_OPTION_COUNT = 3
