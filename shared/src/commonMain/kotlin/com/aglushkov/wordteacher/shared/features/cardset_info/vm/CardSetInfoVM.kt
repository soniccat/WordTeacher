package com.aglushkov.wordteacher.shared.features.cardset_info.vm

import com.aglushkov.wordteacher.shared.analytics.AnalyticEvent
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.events.FocusViewItemEvent
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVMImpl
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVMImpl.FocusLastType
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVMImpl.PendingEvent
import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.StringDescThrowable
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.WebLinkOpener
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.model.CardSet
import com.aglushkov.wordteacher.shared.model.CardSetInfo
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetRepository
import com.aglushkov.wordteacher.shared.res.MR
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.ResourceStringDesc
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime

interface CardSetInfoVM: Clearable {
    var router: CardSetInfoRouter?
//    val state: State
    val uiStateFlow: StateFlow<Resource<UIState>>

    fun onTryAgainPressed()
    fun onNameChanged(name: String)
    fun onDescriptionChanged(description: String)
    fun onSourceChanged(source: String)
    fun onIsAvailableInSearchChanged(isAvailableInSearch: Boolean)
    fun onLinkClicked(link: String)
    fun onImportArticleClicked(link: String)
    fun onTagTextChanged(newText: String, index: Int)
    fun onTagDeleted(index: Int)
    fun onAddTagPressed()
    fun onFocusEventHandled()

    @Serializable
    sealed interface State {
        val isRemoteCardSet: Boolean

        @Serializable
        data class LocalCardSet(
            val id: Long
        ) : State {
            override val isRemoteCardSet: Boolean = false
        }
        @Serializable
        data class RemoteCardSet(
            val cardSet: CardSet,
        ) : State {
            override val isRemoteCardSet: Boolean = true
        }
    }

    data class InputState(
        val name: String? = null,
        val description: String? = null,
        val source: String? = null,
        val isAvailableInSearch: Boolean? = null,
        val tags: List<String>? = null,
    ) {
        val isNameValid: Boolean
            get() = name == null || name.isNotEmpty()
        val validatedName: String?
            get() = if (isNameValid) {
                name
            } else {
                null
            }
        val isNull: Boolean
            get() = name == null &&
                    description == null &&
                    source == null &&
                    isAvailableInSearch == null
    }

    data class UIState(
        val name: String,
        val nameError: StringDesc?,
        val description: String,
        val source: String?,
        val sourceLinks: List<Link>,
        val isAvailableInSearch: Boolean,
        val isEditable: Boolean,
        val tags: List<String>,
        val focusEvent: Event?
    )

    data class Link(
        val span: LinkSpan,
        val canImport: Boolean,
    )

    data class LinkSpan(
        val start: Int,
        val end: Int
    )
}

open class CardSetInfoVMImpl(
    restoredState: CardSetInfoVM.State,
    private val databaseCardWorker: DatabaseCardWorker,
    private val cardSetRepository: CardSetRepository,
    private val webLinkOpener: WebLinkOpener,
    private val analytics: Analytics,
): ViewModel(), CardSetInfoVM {

    override var router: CardSetInfoRouter? = null
    val state: CardSetInfoVM.State = restoredState
    private var focusEvent = MutableStateFlow<Event?>(null)

    private val cardSetState = MutableStateFlow<Resource<CardSet>>(Resource.Uninitialized())
    private val inputState = MutableStateFlow(CardSetInfoVM.InputState())
    override val uiStateFlow: StateFlow<Resource<CardSetInfoVM.UIState>> = combine(
        if (state.isRemoteCardSet) {
            cardSetState
        } else {
            combine(
                cardSetState,
                databaseCardWorker.untilFirstEditingFlow()
            ) { cardSetState, workerState ->
                if (workerState == DatabaseCardWorker.State.EDITING) {
                    cardSetState
                } else {
                    Resource.Loading()
                }
            }
        },
        inputState,
        focusEvent,
    ) { cardSetRes, inputState, focusEvent ->
        cardSetRes.mapLoadedData(
            errorTransformer = {
                StringDescThrowable(ResourceStringDesc(MR.strings.cardset_info_error), it)
            }
        ) { cardSet: CardSet ->
            val source = inputState.source ?: cardSet.info.source

            CardSetInfoVM.UIState(
                name = inputState.validatedName ?: cardSet.name,
                nameError = if (inputState.isNameValid) {
                    null
                } else {
                    StringDesc.Resource(MR.strings.cardset_info_error_empty_name)
                },
                description = inputState.description ?: cardSet.info.description,
                source = source,
                sourceLinks = findLinkSpans(source.orEmpty()).map {
                    CardSetInfoVM.Link(
                        span = it,
                        canImport = !source.orEmpty().substring(it.start, it.end).isFileLink(),
                    )
                },
                isAvailableInSearch = inputState.isAvailableInSearch ?: cardSet.isAvailableInSearch,
                isEditable = !state.isRemoteCardSet,
                tags = inputState.tags ?: cardSet.tags,
                focusEvent = focusEvent,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), Resource.Uninitialized())

    init {
        addClearable(databaseCardWorker.startEditing())
        loadCardSet()

        var hasEditing = false
        if (!state.isRemoteCardSet) {
            // subscribe on input state to update db data
            viewModelScope.launch {
                inputState.collect { lastInputState ->
                    cardSetState.value.data()?.let { dbCardSet ->
                        if (!hasEditing) {
                            hasEditing = hasEditing || lastInputState.name != null && lastInputState.name != dbCardSet.name
                            hasEditing = hasEditing || lastInputState.description != null && lastInputState.description != dbCardSet.info.description
                            hasEditing = hasEditing || lastInputState.source != null && lastInputState.source != dbCardSet.info.source
                            hasEditing = hasEditing || lastInputState.isAvailableInSearch != null && lastInputState.isAvailableInSearch != dbCardSet.isAvailableInSearch
                            hasEditing = hasEditing || lastInputState.tags != null && lastInputState.tags != dbCardSet.tags
                        }

                        if (hasEditing) {
                            databaseCardWorker.updateCardSetInfo(
                                dbCardSet.copy(
                                    name = lastInputState.validatedName ?: dbCardSet.name,
                                    info = dbCardSet.info.copy(
                                        description = lastInputState.description
                                            ?: dbCardSet.info.description,
                                        source = lastInputState.source ?: dbCardSet.info.source,
                                    ),
                                    isAvailableInSearch = lastInputState.isAvailableInSearch
                                        ?: dbCardSet.isAvailableInSearch,
                                    tags = lastInputState.tags ?: dbCardSet.tags
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onTryAgainPressed() {
        loadCardSet()
    }

    override fun onNameChanged(name: String) {
        logChange("name")
        inputState.update { it.copy(name = name) }
    }

    override fun onDescriptionChanged(description: String) {
        logChange("description")
        inputState.update { it.copy(description = description) }
    }

    override fun onSourceChanged(source: String) {
        logChange("source")
        inputState.update { it.copy(source = source) }
    }

    override fun onIsAvailableInSearchChanged(isAvailableInSearch: Boolean) {
        logChange("isAvailableInSearchChanged")
        inputState.update { it.copy(isAvailableInSearch = isAvailableInSearch) }
    }

    override fun onLinkClicked(link: String) {
        webLinkOpener.open(link)
    }

    override fun onImportArticleClicked(link: String) {
        router?.openAddArticle(link, showNeedToCreateCardSet = false)
    }

    private fun logChange(fieldType: String) {
        analytics.send(AnalyticEvent.createActionEvent("CardSetInfo.change", mapOf("fieldType" to fieldType)))
    }

    private fun loadCardSet() {
        viewModelScope.launch(Dispatchers.IO) {
            when (state) {
                is CardSetInfoVM.State.LocalCardSet -> {
                    cardSetRepository.loadCardSetWithoutCards(state.id).collect(cardSetState)
                }
                is CardSetInfoVM.State.RemoteCardSet -> {
                    cardSetState.value = Resource.Loaded(
                        CardSet(
                            id = 0,
                            remoteId = "",
                            name = state.cardSet.name,
                            creationDate = Instant.DISTANT_PAST,
                            modificationDate = Instant.DISTANT_PAST,
                            cards = listOf(),
                            terms = listOf(),
                            creationId = "",
                            info = CardSetInfo(
                                description = state.cardSet.info.description,
                                source = state.cardSet.info.source
                            ),
                            isAvailableInSearch = false,
                            tags = state.cardSet.tags,
                        )
                    )
                }
            }
        }
    }

    // tags

    override fun onTagTextChanged(newText: String, index: Int) {
        logChange("onTagTextChanged")
        val currentTags = inputState.value.tags ?: cardSetState.value.data()?.tags
        if (currentTags.isNullOrEmpty()) {
            return
        }

        inputState.update {
            it.copy(
                tags = currentTags.mapIndexed { i, v ->
                    if (index == i) {
                        newText
                    } else {
                        v
                    }
                }
            )
        }
    }

    override fun onTagDeleted(index: Int) {
        logChange("onTagDeleted")
        val currentTags = inputState.value.tags ?: cardSetState.value.data()?.tags
        if (currentTags.isNullOrEmpty()) {
            return
        }

        inputState.update {
            it.copy(
                tags = currentTags.filterIndexed { i, v ->
                    i != index
                }
            )
        }
    }

    override fun onAddTagPressed() {
        logChange("onAddTagPressed")
        val currentTags = inputState.value.tags ?: cardSetState.value.data()?.tags
        inputState.update {
            it.copy(
                tags = currentTags.orEmpty() + "",
            )
        }
        focusEvent.update { FocusLastEvent(ElementType.Tag) }
    }

    override fun onFocusEventHandled() {
        focusEvent.update { null }
    }

    open class FocusLastEvent(
        val type: ElementType,
        override var isHandled: Boolean = false
    ): Event {
        override fun markAsHandled() {
            isHandled = true
        }
    }

    enum class ElementType {
        Tag,
    }
}

val LinkDividerCharCategories = setOf(
    CharCategory.SPACE_SEPARATOR,
)

// TODO: check how it works in Linkify.addLinks, probably can copy their solution
private fun findLinkSpans(str: String): List<CardSetInfoVM.LinkSpan> {
    if (str.isEmpty()) return emptyList()

    var i = 0
    val linkSpans = mutableListOf<CardSetInfoVM.LinkSpan>()
    val prefix = "http"
    while (i < str.length) {
        val foundI = str.indexOf(prefix, i)
        if (foundI == -1) {
            break
        }

        val linkEnd = str.indexOfChar(foundI + prefix.length) {
            LinkDividerCharCategories.contains(it.category)
        }
        if (linkEnd == -1) {
            linkSpans.add(CardSetInfoVM.LinkSpan(foundI, str.length))
            break
        }

        linkSpans.add(CardSetInfoVM.LinkSpan(foundI, linkEnd))
        i = linkEnd
    }

    return linkSpans
}

fun String.indexOfChar(start: Int, checker: (Char) -> Boolean): Int {
    var i = start
    while (i < length) {
        if (checker(get(i))) {
            return i
        }

        ++i
    }

    return -1
}

fun String.isFileLink(): Boolean {
    val i = indexOfLast { it == '.' }
    if (i == -1) {
        return false
    }

    val ext = substring(i)
    return when (ext) {
        "pdf", "doc", "xml" -> true
        else -> false
    }
}