package com.aglushkov.wordteacher.shared.features.learning.vm

import com.aglushkov.wordteacher.shared.features.definitions.vm.Indent
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordDefinitionViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordExampleViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordPartOfSpeechViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordSubHeaderViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordSynonymViewItem
import com.aglushkov.wordteacher.shared.features.learning.cardteacher.CardTeacher
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.model.toStringDesc
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.db.DatabaseWorker
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface LearnVM {
    val viewItems: StateFlow<List<BaseViewItem<*>>>

    suspend fun onCheckPressed(answer: String)
    fun onTextChanged()
    fun onShowNextLetterPressed()
    fun onShowRandomLetterPressed()
    suspend fun onGiveUpPressed()
}

class LearnVMImpl(
    private val cards: List<Card>,
    private val database: AppDatabase,
    private val databaseWorker: DatabaseWorker,
    private val timeSource: TimeSource,
) : ViewModel(), LearnVM {

    override val viewItems = MutableStateFlow<List<BaseViewItem<*>>>(emptyList())

    private val teacher = CardTeacher(
        cards,
        database,
        databaseWorker,
        timeSource,
        viewModelScope
    )

    init {
        viewModelScope.launch {
            teacher.buildCourseSession()
            teacher.currentSessionFlow.collect { learnSession ->
                teacher.currentCardFlow.collect { card ->
                    viewItems.value = buildCardItem(card)
                }

                // create result items
                //it.results
            }
        }
    }

    private fun buildCardItem(card: Card): List<BaseViewItem<*>> {
        return listOf(
            LearnInputViewItem(card.id),
            WordPartOfSpeechViewItem(card.partOfSpeech.toStringDesc()),
            *card.definitions.map { def ->
                WordDefinitionViewItem(definition = def.replace(card.term, TERM_REPLACEMENT))
            }.toTypedArray(),

            WordSubHeaderViewItem(
                StringDesc.Resource(MR.strings.word_section_examples),
                Indent.SMALL
            ),
            *card.examples.map { ex ->
                WordExampleViewItem(ex.replace(card.term, TERM_REPLACEMENT), Indent.SMALL)
            }.toTypedArray(),

            WordSubHeaderViewItem(
                StringDesc.Resource(MR.strings.word_section_synonyms),
                Indent.SMALL
            ),
            *card.synonyms.map { synonym ->
                WordSynonymViewItem(synonym.replace(card.term, TERM_REPLACEMENT), Indent.SMALL)
            }.toTypedArray()
        )
    }

    override suspend fun onCheckPressed(answer: String) {
        teacher.onCheckInput(answer)
    }

    override fun onTextChanged() {
        TODO("Not yet implemented")
    }

    override fun onShowNextLetterPressed() {
        TODO("Not yet implemented")
    }

    override fun onShowRandomLetterPressed() {
        TODO("Not yet implemented")
    }

    override suspend fun onGiveUpPressed() {
        teacher.onGiveUp()
    }
}

private const val TERM_REPLACEMENT = "__"
