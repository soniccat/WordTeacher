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
    private val matchPairStateFlow = MutableStateFlow<List<MatchPair>?>(null)
    val matchPairFlow: Flow<List<MatchPair>?> = matchPairStateFlow

    private var currentSelectionGroup: Int = 0
    private var lastSelectedTermIndex: Int = -1
    private var lastSelectedExampleIndex: Int = -1

    init {
        val indexToExample = cards.mapIndexed { index, card ->
            val exampleIndex = Random.nextInt(card.examples.size)
            val exampleWithGaps = resolveStringWithHiddenSpans(card.examples[exampleIndex], card.exampleTermSpans[exampleIndex])
            index to exampleWithGaps
        }.shuffled().toMutableList()

        matchPairStateFlow.update {
            cards.mapIndexed { index, card ->
                var randExampleIndex = indexToExample.indexOfFirst { it.first != index }
                if (randExampleIndex == -1) {
                    randExampleIndex = 0
                }
                val example = indexToExample.removeAt(randExampleIndex).second
                MatchPair(card.term, example, index)
            }
        }
    }

    fun selectTerm(pair: MatchPair) {
        val index = matchPairStateFlow.value?.indexOf(pair) ?: return
        if (index == -1) {
            return
        }

//        var groupToClear: Int = -1
        if (!pair.termSelection.isSelected) {
            lastSelectedTermIndex = index
        } else {
//            groupToClear = pair.termSelection.group
            if (lastSelectedTermIndex == index) {
                lastSelectedTermIndex = -1
            }
        }

        // update termSelection
        var updatedPair: MatchPair? = null
        updatePairs { i, p ->
            if (i == index) {
                if (!pair.termSelection.isSelected) {
                    val newPair = pair.copy(
                        termSelection = MatchSelection(
                            isSelected = !pair.termSelection.isSelected,
                            oppositeSelectedIndex = lastSelectedExampleIndex,
                            group = currentSelectionGroup,
                        ),
                    )
                    if (newPair.termSelection.hasMatch()) {
                        ++currentSelectionGroup
                        lastSelectedTermIndex = -1
                        lastSelectedExampleIndex = -1
                    }
                    updatedPair = newPair
                    newPair
                } else {
                    pair.copy(
                        termSelection = MatchSelection()
                    )
                }
            } else if (p.exampleSelection.oppositeSelectedIndex == index) {
                // TODO: it seems here we need to clear selection with the same group and above
                p.copy(exampleSelection = p.exampleSelection.copy(oppositeSelectedIndex = -1, group = -1))
            } else {
                p
            }
        }

        // update opposite exampleSelection
        updatedPair?.let { up ->
            updatePairs { i, p ->
                if (i == up.termSelection.oppositeSelectedIndex) {
                    p.copy(exampleSelection = p.exampleSelection.copy(oppositeSelectedIndex = index))
                } else {
                    p
                }
            }
        }
    }

    fun selectExample(pair: MatchPair) {
        val index = matchPairStateFlow.value?.indexOf(pair) ?: return
        if (index == -1) {
            return
        }

        if (!pair.exampleSelection.isSelected) {
            lastSelectedExampleIndex = index
        } else if (lastSelectedExampleIndex == index) {
            lastSelectedExampleIndex = -1
        }

        // update example selection
        var updatedPair: MatchPair? = null
        updatePairs { i, p ->
            if (i == index) {
                if (!pair.exampleSelection.isSelected) {
                    val newPair = pair.copy(
                        exampleSelection = MatchSelection(
                            isSelected = true,
                            oppositeSelectedIndex = lastSelectedTermIndex,
                            group = currentSelectionGroup,
                        ),
                    )
                    if (newPair.exampleSelection.hasMatch()) {
                        ++currentSelectionGroup
                        lastSelectedTermIndex = -1
                        lastSelectedExampleIndex = -1
                    }
                    updatedPair = newPair
                    newPair
                } else {
                    pair.copy(
                        exampleSelection = MatchSelection()
                    )
                }
            } else if (p.termSelection.oppositeSelectedIndex == index) {
                // TODO: it seems here we need to clear selection with the same group and above
                p.copy(termSelection = p.termSelection.copy(oppositeSelectedIndex = -1, group = -1))
            } else {
                p
            }
        }

        // update opposite termSelection
        updatedPair?.let { up ->
            updatePairs { i, p ->
                if (i == up.exampleSelection.oppositeSelectedIndex) {
                    p.copy(termSelection = p.termSelection.copy(oppositeSelectedIndex = index))
                } else {
                    p
                }
            }
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
        var termSelection: MatchSelection = MatchSelection(),
        var exampleSelection: MatchSelection = MatchSelection(),
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

        fun hasMatch(): Boolean {
            return termSelection.hasMatch() || exampleSelection.hasMatch()
        }
    }

    data class MatchSelection(
        val isSelected: Boolean = false,
        val oppositeSelectedIndex: Int = -1,
        val group: Int = -1,
    ) {
        fun hasMatch(): Boolean {
            return isSelected && oppositeSelectedIndex != -1
        }
    }
}
