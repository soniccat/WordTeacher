package com.aglushkov.wordteacher.androidApp.features

import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.shared.repository.data_loader.CardLoader
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.db.DatabaseWorker
import dagger.Module
import dagger.Provides

@Module
class CardLoaderModule {

    @Provides
    fun provideCardLoader(
        database: AppDatabase,
        databaseWorker: DatabaseWorker
    ) = CardLoader(
        database,
        databaseWorker
    )
}