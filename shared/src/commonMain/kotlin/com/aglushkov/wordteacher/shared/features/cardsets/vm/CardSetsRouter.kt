package com.aglushkov.wordteacher.shared.features.cardsets.vm

interface CardSetsRouter {
    fun openCardSet(id: Long)
    fun openLearning(ids: List<Long>)
    fun openJsonImport()
}