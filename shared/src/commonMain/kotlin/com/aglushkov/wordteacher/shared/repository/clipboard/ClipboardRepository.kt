package com.aglushkov.wordteacher.shared.repository.clipboard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class ClipboardRepository {
    val clipData = MutableStateFlow(Data(""))

    fun setText(text: String) {
        clipData.update {
            Data(text)
        }
    }

    data class Data(val text: String) {
        val isEmpty: Boolean
            get() = text.isEmpty()
    }
}
