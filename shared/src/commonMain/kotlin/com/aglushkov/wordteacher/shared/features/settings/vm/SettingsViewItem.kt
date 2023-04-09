package com.aglushkov.wordteacher.shared.features.settings.vm

import com.aglushkov.wordteacher.shared.features.definitions.vm.WordTitleViewItem
import com.aglushkov.wordteacher.shared.features.notes.vm.NoteViewItem
import dev.icerock.moko.resources.desc.StringDesc
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.config.Config
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf


data class SettingsViewTitleItem(
    val title: StringDesc,
    override val id: Long = 0L,
    override val type: Int = 1000,
    override val items: ImmutableList<StringDesc> = persistentListOf(title),
): BaseViewItem<StringDesc> {
    override fun copyWithId(id: Long): BaseViewItem<StringDesc> = this.copy(id = id)
}

data class SettingsViewLoading(
    override val id: Long = 0L,
    override val type: Int = 1001,
    override val items: ImmutableList<Unit> = persistentListOf(),
): BaseViewItem<Unit> {
    override fun copyWithId(id: Long): BaseViewItem<Unit> = this.copy(id = id)
}

data class SettingsViewAuthButtonItem(
    val text: StringDesc,
    val buttonType: ButtonType,
    override val id: Long = 0L,
    override val type: Int = 1002,
    override val items: ImmutableList<StringDesc> = persistentListOf(text),
): BaseViewItem<StringDesc> {
    enum class ButtonType {
        SignIn,
        SignOut,
        TryAgain
    }

    override fun copyWithId(id: Long): BaseViewItem<StringDesc> = this.copy(id = id)

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        return super.equalsByContent(other) && buttonType == (other as SettingsViewAuthButtonItem).buttonType
    }
}

data class SettingsViewAuthRefreshButtonItem(
    val text: StringDesc,
    override val id: Long = 0L,
    override val type: Int = 1003,
    override val items: ImmutableList<StringDesc> = persistentListOf(text),
): BaseViewItem<StringDesc> {
    companion object {
        const val Type = 1003
    }

    override fun copyWithId(id: Long): BaseViewItem<StringDesc> = this.copy(id = id)
}

