package com.aglushkov.wordteacher.androidApp.features.cardset.di

import com.aglushkov.wordteacher.androidApp.general.RouterResolver
import com.aglushkov.wordteacher.shared.features.cardset.CardSetDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetRouter
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.features.notes.NotesDecomposeComponent
import com.aglushkov.wordteacher.shared.features.notes.vm.NotesVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.article.ArticleRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetRepository
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.note.NotesRepository
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides

@Module
class CardSetModule {

    @Provides
    fun cardSetRepository(
        database: AppDatabase,
        timeSource: TimeSource
    ) = CardSetRepository(database, timeSource)

    @Provides
    fun cardSetDecomposeComponent(
        state: CardSetVM.State,
        routerResolver: RouterResolver,
        notesRepository: CardSetRepository,
        componentContext: ComponentContext,
        timeSource: TimeSource,
        idGenerator: IdGenerator
    ) = CardSetDecomposeComponent(
        state,
        routerResolver.router!!.get()!!,
        notesRepository,
        componentContext,
        timeSource,
        idGenerator
    )
}
