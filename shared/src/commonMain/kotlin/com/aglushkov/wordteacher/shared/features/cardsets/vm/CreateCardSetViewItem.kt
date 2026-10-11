package com.aglushkov.wordteacher.shared.features.cardsets.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.StringResourceWrapper
import com.aglushkov.wordteacher.shared.model.Article
import org.jetbrains.compose.resources.StringResource

class CreateCardSetViewItem(
    val placeholder: StringResourceWrapper,
    val text: String = ""
): BaseViewItem<Unit>(Unit, Type, -1) {
    companion object {
        const val Type = 601
    }
}
