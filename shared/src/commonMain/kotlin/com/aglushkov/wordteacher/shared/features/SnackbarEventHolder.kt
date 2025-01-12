package com.aglushkov.wordteacher.shared.features

import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

interface SnackbarEventHolderRouter {
    fun openArticle(id: Long)
    fun openLocalCardSet(cardSetId: Long)
}

data class SnackbarEventHolderItem(
    val flow: StateFlow<List<SnackbarEventHolder.Event>>,
    val handler: (SnackbarEventHolder.Event, withAction: Boolean) -> Unit,
)

interface SnackbarEventHolder {
    companion object {
        val map = mutableMapOf<String, SnackbarEventHolderItem>()

        fun addSource(name: String, item: SnackbarEventHolderItem) {
            map[name] = item
        }

        fun removeSource(name: String) {
            map.remove(name)
        }
    }

    var snackbarEventRouter: SnackbarEventHolderRouter?
    val events: StateFlow<List<Event>>

    fun onArticleCreated(articleId: Long)
    fun onCardSetUpdated(cardSetId: Long)
    fun onError(text: StringDesc)
    fun onEventHandled(event: Event, withAction: Boolean)

    sealed interface Event {
        val text: StringDesc
        val actionText: StringDesc?
            get() = null

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

        data class ErrorEvent(override val text: StringDesc): Event
    }
}

class SnackbarEventHolderImpl: SnackbarEventHolder {
    override var snackbarEventRouter: SnackbarEventHolderRouter? = null
    override val events = MutableStateFlow(listOf<SnackbarEventHolder.Event>())

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

    override fun onError(text: StringDesc) {
        events.update {
            it + SnackbarEventHolder.Event.ErrorEvent(text)
        }
    }

    override fun onEventHandled(event: SnackbarEventHolder.Event, withAction: Boolean) {
        events.update {
            it.filter { e -> e != event }
        }
        when (event) {
            is SnackbarEventHolder.Event.OpenArticleEvent -> onOpenArticleEventHandled(event, withAction)
            is SnackbarEventHolder.Event.CardSetUpdatedEvent -> onCardSetUpdatedEvent(event, withAction)
            is SnackbarEventHolder.Event.ErrorEvent -> Unit
        }
    }

    private fun createOpenArticleEvent(id: Long) = SnackbarEventHolder.Event.OpenArticleEvent(
        text = StringDesc.Resource(MR.strings.articles_action_article_created),
        openText = StringDesc.Resource(MR.strings.articles_action_open),
        id = id,
    )

    private fun createCardSetUpdatedEvent(id: Long) = SnackbarEventHolder.Event.CardSetUpdatedEvent(
        text = StringDesc.Resource(MR.strings.definitions_cardsets_card_added),
        openText = StringDesc.Resource(MR.strings.definitions_cardsets_open),
        id = id
    )

    private fun onOpenArticleEventHandled(
        event: SnackbarEventHolder.Event.OpenArticleEvent,
        needOpen: Boolean,
    ) {
        if (needOpen) {
            snackbarEventRouter?.openArticle(event.id)
        }
    }

    private fun onCardSetUpdatedEvent(
        event: SnackbarEventHolder.Event.CardSetUpdatedEvent,
        needOpen: Boolean,
    ) {
        if (needOpen) {
            snackbarEventRouter?.openLocalCardSet(event.id)
        }
    }
}