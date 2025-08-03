package com.aglushkov.wordteacher.shared.features.settings.vm

import dev.icerock.moko.resources.desc.StringDesc
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyGradation
import com.aglushkov.wordteacher.shared.service.SpaceAuthService
import okio.Path


class SettingsViewTitleItem(title: StringDesc): BaseViewItem<StringDesc>(title, Type) {
    companion object {
        const val Type = 1000
    }
}

class SettingsViewTextItem(
    title: StringDesc,
    val withBottomPadding: Boolean = true,
): BaseViewItem<StringDesc>(title, Type) {
    companion object {
        const val Type = 1001
    }
}

class SettingsViewLoading: BaseViewItem<Unit>(Unit, Type) {
    companion object {
        const val Type = 1002
    }
}

class SettingsSignInItem(
    val networkTypes: List<SpaceAuthService.NetworkType>,
): BaseViewItem<Unit>(Unit, Type) {
    companion object {
        const val Type = 1003
    }

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        return super.equalsByContent(other) &&
                networkTypes == (other as SettingsSignInItem).networkTypes
    }
}

class SettingsSignOutItem(text: StringDesc): BaseViewItem<StringDesc>(text, Type) {
    companion object {
        const val Type = 1009
    }
}

class SettingsViewAuthRefreshButtonItem(text: StringDesc): BaseViewItem<StringDesc>(text, Type) {
    companion object {
        const val Type = 1004
    }
}

class SettingsOpenDictConfigsItem: BaseViewItem<Unit>(Unit, Type) {
    companion object {
        const val Type = 1005
    }
}

class SettingsLogsConfigsItem(
    val isEnabled: Boolean,
    val paths: List<LogFileItem>
): BaseViewItem<Boolean>(isEnabled, Type) {
    companion object {
        const val Type = 1006
    }

    data class LogFileItem(val path: Path)
}

class SettingsWordFrequencyGradationItem(
    val gradation: WordFrequencyGradation,
): BaseViewItem<WordFrequencyGradation>(gradation, Type) {
    companion object {
        const val Type = 1007
    }
}

class SettingsWordFrequencyUploadFileItem(
    text: StringDesc,
): BaseViewItem<StringDesc>(text, Type) {
    companion object {
        const val Type = 1008
    }
}

class SettingsAbout(
    val appTitle: String,
    val email: String,
): BaseViewItem<Unit>(Unit, Type) {
    companion object {
        const val Type = 1009
    }
}

class SettingsPrivacyPolicyItem(
): BaseViewItem<Unit>(Unit, Type) {
    companion object {
        const val Type = 1011
    }
}

class SettingsResetHintsItem(
    text: StringDesc,
): BaseViewItem<StringDesc>(text, Type) {
    companion object {
        const val Type = 1012
    }
}
