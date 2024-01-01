package com.aglushkov.wordteacher.shared.repository.data_loader

import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isError
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.workers.DatabaseWorker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

// TODO: refactor to ResourceRepository
class CardLoader(
    private val database: AppDatabase,
    private val databaseWorker: DatabaseWorker
) {
    private val retryStatFlow = MutableStateFlow(0)

    suspend fun loadCardsUntilLoaded(
        cardIds: List<Long>,
        onLoading: () -> Unit,
        onError: (e: Throwable) -> Unit
    ): List<Card> {
        while (true) {
            var res: Resource<List<Card>>? = Resource.Uninitialized()
            loadCards(cardIds).collect {
                res = it
                when (it) {
                    is Resource.Loading -> onLoading()
                    is Resource.Error -> onError(it.throwable)
                    else -> { }
                }
            }

            val safeRes = res
            if (safeRes is Resource.Loaded) {
                return safeRes.data
            } else if (safeRes.isError()) {
                // wait for user interaction
                retryStatFlow.first()
            }
        }
    }

    fun tryLoadCardsAgain() {
        retryStatFlow.value++
    }

    private suspend fun loadCards(cardIds: List<Long>): Flow<Resource<List<Card>>> = flow {
        emit(Resource.Loading())
        try {
            val cards = databaseWorker.run {
                database.cards.selectCards(cardIds).executeAsList()
            }
            emit(Resource.Loaded(cards))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            emit(Resource.Error(e, canTryAgain = true))
        }
    }
}
