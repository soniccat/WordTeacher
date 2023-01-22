package com.aglushkov.wordteacher.android_app.features

import com.aglushkov.wordteacher.shared.repository.data_loader.CardLoader
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.workers.DatabaseWorker
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