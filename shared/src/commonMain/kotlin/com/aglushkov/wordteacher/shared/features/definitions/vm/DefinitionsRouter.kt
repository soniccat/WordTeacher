package com.aglushkov.wordteacher.shared.features.definitions.vm

import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.general.SimpleRouter

interface DefinitionsRouter {
    fun openCardSets()
    fun onLocalCardSetUpdated(cardSetId: Long)
    fun onDefinitionsClosed()
}