package com.aglushkov.wordteacher.shared.repository.cardset

import com.aglushkov.wordteacher.shared.general.extensions.asFlow
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.tryInResource
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.flow.stateIn

class CardsRepository(
    private val database: AppDatabase
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val cards = database.cards.selectAllCards().asFlow().map {
        tryInResource(canTryAgain = true) { it.executeAsList() }
    }.stateIn(scope, SharingStarted.Eagerly, Resource.Uninitialized())

    fun cancel() {
        scope.cancel()
    }
}
