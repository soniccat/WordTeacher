package com.aglushkov.wordteacher.shared.repository.clipboard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class ClipboardRepository {
    val clipData = MutableStateFlow(Data(""))
    var lastTextHash: Int = 0

    fun setText(text: String, textHash: Int) {
        if (lastTextHash == textHash) {
            return
        }

        lastTextHash = textHash
        clipData.update {
            Data(text)
        }
    }

    data class Data(val text: String) {
        val isEmpty: Boolean
            get() = text.isEmpty()
    }
}
