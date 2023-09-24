package com.aglushkov.wordteacher.shared.features.dict_configs.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem

class YandexConfigViewItem(
    id: Long,
    val hasToken: Boolean,
    val lang: String,
    val settings: YandexSettings
): BaseViewItem<Unit>(Unit, Type, id) {
    companion object {
        const val Type = 1100
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other) && run {
            other as YandexConfigViewItem
            hasToken == other.hasToken && lang == other.lang && settings == other.settings
        }
    }
}

class CreateConfigViewItem(
    id: Long,
): BaseViewItem<Unit>(Unit, Type, id) {
    companion object {
        const val Type = 1101
    }
}
