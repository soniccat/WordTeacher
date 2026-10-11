package com.aglushkov.wordteacher.shared.features.definitions.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.StringResourceWrapper
import com.aglushkov.wordteacher.shared.model.Article
import org.jetbrains.compose.resources.StringResource

class OpenCardSetViewItem(
    val text: StringResourceWrapper
): BaseViewItem<Unit>(Unit, Type, -1) {
    companion object {
        const val Type = 120
    }
}

