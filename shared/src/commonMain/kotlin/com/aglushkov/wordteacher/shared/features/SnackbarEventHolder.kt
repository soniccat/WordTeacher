package com.aglushkov.wordteacher.shared.features

import com.aglushkov.wordteacher.shared.general.resource.StringResourceWrapper
import wordteacher.shared.generated.resources.Res
import org.jetbrains.compose.resources.StringResource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import wordteacher.shared.generated.resources.articles_action_article_created
import wordteacher.shared.generated.resources.articles_action_open
import wordteacher.shared.generated.resources.cardsets_search_added
import wordteacher.shared.generated.resources.cardsets_search_added_open
import wordteacher.shared.generated.resources.cardsets_search_try_again
import wordteacher.shared.generated.resources.definitions_cardsets_card_added
import wordteacher.shared.generated.resources.definitions_cardsets_open

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
    fun onError(text: StringResourceWrapper, actionText: StringResourceWrapper? = null, onActionCalled: (() -> Unit)? = null)
    fun onCardSetCreated(cardSetId: Long, name: String)
    fun onCardSetLoadingError(remoteId: String, name: String, onActionCalled: (() -> Unit)?)

    fun onEventHandled(event: Event, withAction: Boolean)

    sealed interface Event {
        val text: StringResourceWrapper
        val actionText: StringResourceWrapper?
            get() = null
        val onActionCalled: (() -> Unit)?
            get() = null

        data class OpenArticleEvent(
            override val text: StringResourceWrapper,
            val openText: StringResourceWrapper,
            val id: Long,
        ) : Event {
            override val actionText: StringResourceWrapper
                get() = openText
        }

        data class CardSetUpdatedEvent(
            override val text: StringResourceWrapper,
            val openText: StringResourceWrapper,
            val id: Long,
        ): Event {
            override val actionText: StringResourceWrapper
                get() = openText
        }

        data class OpenCardSetEvent(
            override val text: StringResourceWrapper,
            val openText: StringResourceWrapper,
            val id: Long,
        ): Event {
            override val actionText: StringResourceWrapper
                get() = openText
        }

        data class CardSetLoadingError(
            override val text: StringResourceWrapper,
            val remoteId: String,
            val reloadText: StringResourceWrapper,
            override val onActionCalled: (() -> Unit)? = null,
        ): Event {
            override val actionText: StringResourceWrapper
                get() = reloadText
        }

        data class ErrorEvent(
            override val text: StringResourceWrapper,
            override val actionText: StringResourceWrapper? = null,
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

    override fun onError(text: StringResourceWrapper, actionText: StringResourceWrapper?, onActionCalled: (() -> Unit)?) {
        events.update {
            it + SnackbarEventHolder.Event.ErrorEvent(text, actionText, onActionCalled)
        }
    }

    override fun onCardSetCreated(cardSetId: Long, name: String) {
        events.update {
            it + SnackbarEventHolder.Event.OpenCardSetEvent(
                text = StringResourceWrapper.FormattedString(Res.string.cardsets_search_added, listOf(name)),
                openText = StringResourceWrapper.String(Res.string.cardsets_search_added_open),
                id = cardSetId,
            )
        }
    }

    override fun onCardSetLoadingError(remoteId: String, name: String, onActionCalled: (() -> Unit)?) {
        events.update {
            it + SnackbarEventHolder.Event.CardSetLoadingError(
                text = StringResourceWrapper.FormattedString(Res.string.cardsets_search_added, listOf(name)),
                remoteId = remoteId,
                reloadText = StringResourceWrapper.String(Res.string.cardsets_search_try_again),
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
        text = StringResourceWrapper.String(Res.string.articles_action_article_created),
        openText = StringResourceWrapper.String(Res.string.articles_action_open),
        id = id,
    )

    private fun createCardSetUpdatedEvent(id: Long) = SnackbarEventHolder.Event.CardSetUpdatedEvent(
        text = StringResourceWrapper.String(Res.string.definitions_cardsets_card_added),
        openText = StringResourceWrapper.String(Res.string.definitions_cardsets_open),
        id = id
    )
}