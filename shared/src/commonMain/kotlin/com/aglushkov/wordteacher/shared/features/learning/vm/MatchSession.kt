package com.aglushkov.wordteacher.shared.features.learning.vm

import com.aglushkov.wordteacher.shared.model.Card
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

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
            index to card.examples.random()
        }.shuffled().toMutableList()
        matchPairStateFlow.update {
            cards.mapIndexed { index, card ->
                val randExampleIndex = indexToExample.indexOfFirst { it.first != index }
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

        updatePairs { i, p ->
            if (i == index) {
                val newPair = pair.copy(
                    termSelection = MatchSelection(
                        isSelected = !pair.termSelection.isSelected,
                        oppositeSelectedIndex = lastSelectedTermIndex,
                        group = currentSelectionGroup,
                    ),
                )
                if (pair.termSelection.hasMatch()) {
                    ++currentSelectionGroup
                }
                newPair
            } else if (p.exampleSelection.oppositeSelectedIndex == index) {
                // TODO: it seems here we need to clear selection with the same group and above
                p.copy(exampleSelection = p.exampleSelection.copy(oppositeSelectedIndex = -1, group = -1))
            } else {
                p
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

        updatePairs { i, p ->
            if (i == index) {
                val newPair = pair.copy(
                    exampleSelection = MatchSelection(
                        isSelected = !pair.exampleSelection.isSelected,
                        oppositeSelectedIndex = lastSelectedTermIndex,
                        group = currentSelectionGroup,
                    ),
                )
                if (pair.exampleSelection.hasMatch()) {
                    ++currentSelectionGroup
                }
                newPair
            } else if (p.termSelection.oppositeSelectedIndex == index) {
                // TODO: it seems here we need to clear selection with the same group and above
                p.copy(termSelection = p.termSelection.copy(oppositeSelectedIndex = -1, group = -1))
            } else {
                p
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
            return result
        }

        override fun equals(other: Any?): Boolean {
            val otherMatchPair = other as? MatchPair ?: return false
            return term == otherMatchPair.term &&
                    example == otherMatchPair.example &&
                    rightExampleIndex == other.rightExampleIndex
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
