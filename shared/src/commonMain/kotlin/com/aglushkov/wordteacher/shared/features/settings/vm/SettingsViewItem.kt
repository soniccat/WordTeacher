package com.aglushkov.wordteacher.shared.features.settings.vm

import com.aglushkov.wordteacher.shared.features.definitions.vm.WordTitleViewItem
import com.aglushkov.wordteacher.shared.features.notes.vm.NoteViewItem
import dev.icerock.moko.resources.desc.StringDesc
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.config.Config


class SettingsViewTitleItem(title: StringDesc): BaseViewItem<StringDesc>(title, Type) {
    companion object {
        const val Type = 1000
    }
}

class SettingsViewLoading: BaseViewItem<Unit>(Unit, Type) {
    companion object {
        const val Type = 1001
    }
}

class SettingsViewAuthButtonItem(text: StringDesc, val buttonType: ButtonType): BaseViewItem<StringDesc>(text, Type) {
    enum class ButtonType {
        SignIn,
        SignOut,
        TryAgain
    }

    companion object {
        const val Type = 1002
    }

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        return super.equalsByContent(other) && buttonType == (other as SettingsViewAuthButtonItem).buttonType
    }
}

class SettingsViewAuthRefreshButtonItem(text: StringDesc): BaseViewItem<StringDesc>(text, Type) {
    companion object {
        const val Type = 1003
    }
}

class SettingsOpenDictConfigsItem: BaseViewItem<Unit>(Unit, Type) {
    companion object {
        const val Type = 1004
    }
}