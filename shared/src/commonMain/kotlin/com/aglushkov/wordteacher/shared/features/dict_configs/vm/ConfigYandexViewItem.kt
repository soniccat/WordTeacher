package com.aglushkov.wordteacher.shared.features.dict_configs.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import dev.icerock.moko.resources.desc.StringDesc
import okio.Path

class ConfigYandexViewItem(
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
            other as ConfigYandexViewItem
            hasToken == other.hasToken && lang == other.lang && settings == other.settings
        }
    }
}

class ConfigHeaderViewItem(
    text: StringDesc
): BaseViewItem<StringDesc>(text, Type) {
    companion object {
        const val Type = 1101
    }
}

class ConfigTextViewItem(
    text: StringDesc
): BaseViewItem<StringDesc>(text, Type) {
    companion object {
        const val Type = 1102
    }
}

class ConfigDictViewItem(
    name: String,
    val path: Path,
): BaseViewItem<String>(name, Type) {
    companion object {
        const val Type = 1103
    }

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        return super.equalsByContent(other) && path == (other as ConfigDictViewItem).path
    }
}

class ConfigLoadingViewItem: BaseViewItem<Unit>(Unit, Type) {
    companion object {
        const val Type = 1004
    }
}

class ConfigCreateViewItem(
    type: Type,
): BaseViewItem<ConfigCreateViewItem.Type>(type, Type) {
    companion object {
        const val Type = 1105
    }

    enum class Type {
        Online,
        Offline
    }
}
