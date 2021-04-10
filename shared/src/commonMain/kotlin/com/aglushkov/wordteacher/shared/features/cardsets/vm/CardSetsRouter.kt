package com.aglushkov.wordteacher.shared.features.cardsets.vm

interface CardSetsRouter {
    fun openAddCardSet()
    fun openCardSet(id: Long)
}