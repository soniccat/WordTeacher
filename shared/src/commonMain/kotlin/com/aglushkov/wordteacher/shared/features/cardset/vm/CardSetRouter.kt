package com.aglushkov.wordteacher.shared.features.cardset.vm

import com.aglushkov.wordteacher.shared.events.Event

interface CardSetRouter {
    fun openLearning()
    fun closeCardSet()
}