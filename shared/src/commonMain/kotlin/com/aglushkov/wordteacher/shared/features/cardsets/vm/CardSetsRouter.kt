package com.aglushkov.wordteacher.shared.features.cardsets.vm

interface CardSetsRouter {
    fun openCardSet(id: Long)
    fun openStartLearning()
}