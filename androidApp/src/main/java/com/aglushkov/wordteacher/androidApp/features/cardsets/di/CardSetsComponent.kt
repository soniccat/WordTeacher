package com.aglushkov.wordteacher.androidApp.features.cardsets.di

import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.features.articles.views.ArticlesFragment
import com.aglushkov.wordteacher.androidApp.features.articles.views.ArticlesAndroidVM
import com.aglushkov.wordteacher.androidApp.features.cardsets.views.CardSetsAndroidVM
import com.aglushkov.wordteacher.androidApp.features.cardsets.views.CardSetsFragment
import com.aglushkov.wordteacher.androidApp.general.RouterResolver
import com.aglushkov.wordteacher.di.ArticlesModule
import com.aglushkov.wordteacher.di.CardSetsModule
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import dagger.BindsInstance
import dagger.Component

@FragmentComp
@Component(dependencies = [CardSetsDependencies::class], modules = [CardSetsModule::class])
interface CardSetsComponent {
    fun injectCardSetsFragment(fragment: CardSetsFragment)
    fun injectViewModelWrapper(fragment: CardSetsAndroidVM)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun setVMWrapper(vmWrapper: CardSetsAndroidVM): Builder

        fun setDeps(deps: CardSetsDependencies): Builder
        fun build(): CardSetsComponent
    }
}

interface CardSetsDependencies {
    fun routerResolver(): RouterResolver
    fun cardSetsRepository(): CardSetsRepository
    fun idGenerator(): IdGenerator
    fun timeSource(): TimeSource
}