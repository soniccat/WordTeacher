package com.aglushkov.wordteacher.shared.features.dict_configs.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem

class YandexConfigViewItem(
    val lang: String,
    val settings: YandexSettings
): BaseViewItem<String>(lang, Type) {
    companion object {
        const val Type = 1100
    }
}

class CreateConfigViewItem(
): BaseViewItem<Unit>(Unit, Type) {
    companion object {
        const val Type = 1101
    }
}
