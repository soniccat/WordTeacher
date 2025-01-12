package com.aglushkov.wordteacher.shared.features

import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.ResourceFormatted
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
    fun onError(text: StringDesc, actionText: StringDesc? = null, onActionCalled: (() -> Unit)? = null)
    fun onCardSetCreated(cardSetId: Long, name: String)
    fun onCardSetLoadingError(remoteId: String, name: String, onActionCalled: (() -> Unit)?)

    fun onEventHandled(event: Event, withAction: Boolean)

    sealed interface Event {
        val text: StringDesc
        val actionText: StringDesc?
            get() = null
        val onActionCalled: (() -> Unit)?
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

        data class OpenCardSetEvent(
            override val text: StringDesc,
            val openText: StringDesc,
            val id: Long,
        ): Event {
            override val actionText: StringDesc
                get() = openText
        }

        data class CardSetLoadingError(
            override val text: StringDesc,
            val remoteId: String,
            val reloadText: StringDesc,
            override val onActionCalled: (() -> Unit)? = null,
        ): Event {
            override val actionText: StringDesc
                get() = reloadText
        }

        data class ErrorEvent(
            override val text: StringDesc,
            override val actionText: StringDesc? = null,
            override val onActionCalled: (() -> Unit)? = null,
        ): Event
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

    override fun onError(text: StringDesc, actionText: StringDesc?, onActionCalled: (() -> Unit)?) {
        events.update {
            it + SnackbarEventHolder.Event.ErrorEvent(text, actionText, onActionCalled)
        }
    }

    override fun onCardSetCreated(cardSetId: Long, name: String) {
        events.update {
            it + SnackbarEventHolder.Event.OpenCardSetEvent(
                text = StringDesc.ResourceFormatted(MR.strings.cardsets_search_added, name),
                openText = StringDesc.Resource(MR.strings.cardsets_search_added_open),
                id = cardSetId,
            )
        }
    }

    override fun onCardSetLoadingError(remoteId: String, name: String, onActionCalled: (() -> Unit)?) {
        events.update {
            it + SnackbarEventHolder.Event.CardSetLoadingError(
                text = StringDesc.ResourceFormatted(MR.strings.cardsets_search_added, name),
                remoteId = remoteId,
                reloadText = StringDesc.Resource(MR.strings.cardsets_search_try_again),
                onActionCalled = onActionCalled
             )
        }
    }

    override fun onEventHandled(event: SnackbarEventHolder.Event, withAction: Boolean) {
        events.update {
            it.filter { e -> e != event }
        }
        if (withAction) {
            when (event) {
                is SnackbarEventHolder.Event.OpenArticleEvent -> snackbarEventRouter?.openArticle(event.id)
                is SnackbarEventHolder.Event.CardSetUpdatedEvent -> snackbarEventRouter?.openLocalCardSet(event.id)
                is SnackbarEventHolder.Event.OpenCardSetEvent -> snackbarEventRouter?.openLocalCardSet(event.id)
                else -> Unit
            }
            event.onActionCalled?.invoke()
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
}