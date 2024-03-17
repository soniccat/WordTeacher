package com.aglushkov.wordteacher.shared.features.cardset_info

import com.aglushkov.wordteacher.shared.features.article.vm.ArticleRouter
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVMImpl
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVM
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVMImpl
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.repository.article.ArticleRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardsRepository
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.statekeeper.consume
import com.russhwolf.settings.coroutines.FlowSettings
import kotlinx.serialization.Serializable

class CardSetInfoDecomposeComponent(
    componentContext: ComponentContext,
    initialState: CardSetInfoVM.State,
    cardSetRepository: CardSetRepository,
    idGenerator: IdGenerator,
) : CardSetInfoVMImpl(
    componentContext.stateKeeper.consume(
        key = KEY_STATE,
        strategy = CardSetInfoVM.State.serializer()
    ) ?: initialState,
    cardSetRepository,
    idGenerator,
), ComponentContext by componentContext {

    init {
        stateKeeper.register(
            key = KEY_STATE,
            strategy = CardSetInfoVM.State.serializer()
        ) { this.state }
    }

    private companion object {
        private const val KEY_STATE = "SAVED_STATE"
    }
}
