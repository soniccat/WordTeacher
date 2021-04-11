package com.aglushkov.wordteacher.di

import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.features.articles.blueprints.ArticleBlueprint
import com.aglushkov.wordteacher.androidApp.features.cardsets.blueprints.CardSetBlueprint
import com.aglushkov.wordteacher.androidApp.general.RouterResolver
import com.aglushkov.wordteacher.androidApp.general.ViewItemBinder
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesRouter
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesVM
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsRouter
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsVM
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import dagger.Module
import dagger.Provides

@Module
class CardSetsModule {
    @FragmentComp
    @Provides
    fun createItemViewBinder(
        cardSetBlueprint: CardSetBlueprint
    ): ViewItemBinder {
        return ViewItemBinder()
            .addBlueprint(cardSetBlueprint)
    }

    @FragmentComp
    @Provides
    fun viewModel(
        routerResolver: RouterResolver,
        cardSetsRepository: CardSetsRepository,
        time: TimeSource
    ): CardSetsVM {
        return CardSetsVM(cardSetsRepository, time, object : CardSetsRouter {
            override fun openAddCardSet() {
                routerResolver.router?.get()?.openAddArticle()
            }

            override fun openCardSet(id: Long) {
                routerResolver.router?.get()?.openArticle(id)
            }
        })
    }
}