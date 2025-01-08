package com.aglushkov.wordteacher.shared.features

import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

interface SnackbarEventsHolderRouter {
    fun openArticle(id: Long)
    fun openLocalCardSet(cardSetId: Long)
}

interface SnackbarEventsHolder {
    var snackbarEventRouter: SnackbarEventsHolderRouter?
    val events: StateFlow<List<Event>>

    fun onArticleCreated(articleId: Long)
    fun onCardSetUpdated(cardSetId: Long)
    fun onEventHandled(event: Event, withAction: Boolean)

    sealed interface Event {
        val text: StringDesc
        val actionText: StringDesc

        data class OpenArticleEvent(
            override val text: StringDesc,
            val openText: StringDesc,
            val id: Long,
        ) : Event {
            override val actionText: StringDesc
                get() = openText
        }

        data class CardSetUpdatedEvent(
            override val text: StringDesc,
            val openText: StringDesc,
            val id: Long,
        ): Event {
            override val actionText: StringDesc
                get() = openText
        }
    }
}

class SnackbarEventsHolderImpl: SnackbarEventsHolder {
    override var snackbarEventRouter: SnackbarEventsHolderRouter? = null
    override val events = MutableStateFlow(listOf<SnackbarEventsHolder.Event>())

    override fun onArticleCreated(articleId: Long) {
        events.update {
            it + createOpenArticleEvent(articleId)
        }
    }

    override fun onCardSetUpdated(cardSetId: Long) {
        events.update {
            it + createCardSetUpdatedEvent(cardSetId)
        }
    }

    override fun onEventHandled(event: SnackbarEventsHolder.Event, withAction: Boolean) {
        when (event) {
            is SnackbarEventsHolder.Event.OpenArticleEvent -> onOpenArticleEventHandled(event, withAction)
            is SnackbarEventsHolder.Event.CardSetUpdatedEvent -> onCardSetUpdatedEvent(event, withAction)
        }
    }

    private fun createOpenArticleEvent(id: Long) = SnackbarEventsHolder.Event.OpenArticleEvent(
        text = StringDesc.Resource(MR.strings.articles_action_article_created),
        openText = StringDesc.Resource(MR.strings.articles_action_open),
        id = id,
    )

    private fun createCardSetUpdatedEvent(id: Long) = SnackbarEventsHolder.Event.CardSetUpdatedEvent(
        text = StringDesc.Resource(MR.strings.definitions_cardsets_card_added),
        openText = StringDesc.Resource(MR.strings.definitions_cardsets_open),
        id = id
    )

    private fun onOpenArticleEventHandled(
        event: SnackbarEventsHolder.Event.OpenArticleEvent,
        needOpen: Boolean,
    ) {
        events.update {
            it.filter { e -> e != event }
        }
        if (needOpen) {
            snackbarEventRouter?.openArticle(event.id)
        }
    }

    private fun onCardSetUpdatedEvent(
        event: SnackbarEventsHolder.Event.CardSetUpdatedEvent,
        needOpen: Boolean,
    ) {
        events.update {
            it.filter { it != event }
        }
        if (needOpen) {
            snackbarEventRouter?.openLocalCardSet(event.id)
        }
    }
}