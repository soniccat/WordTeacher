package com.aglushkov.wordteacher.shared.features.cardset.vm

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize

interface CardSetVM {
    @Parcelize
    data class State (
        val cardSetId: Long
    ): Parcelable
}

open class CardSetVMImpl(
): CardSetVM {

}

