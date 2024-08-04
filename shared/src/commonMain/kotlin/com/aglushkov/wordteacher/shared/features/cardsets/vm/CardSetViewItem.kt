package com.aglushkov.wordteacher.shared.features.cardsets.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import dev.icerock.moko.resources.desc.StringDesc

class CardSetViewItem(
    val cardSetId: Long,
    val name: String,
    val date: String,
    val readyToLearnProgress: Float = 0f,
    val totalProgress: Float = 0f
): BaseViewItem<Long>(cardSetId, Type) {
    companion object {
        const val Type = 400
    }

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        other as CardSetViewItem
        return super.equalsByContent(other) &&
                name == other.name &&
                date == other.date &&
                readyToLearnProgress == other.readyToLearnProgress &&
                totalProgress == other.totalProgress
    }
}

class RemoteCardSetViewItem(
    val remoteCardSetId: String,
    val name: String,
    val terms: List<String>,
    val isLoading: Boolean,
): BaseViewItem<String>(name, Type) {
    companion object {
        const val Type = 401
    }

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        other as RemoteCardSetViewItem
        return super.equalsByContent(other) && terms == other.terms
    }
}

class SectionViewItem(
    val name: StringDesc,
    val isTop: Boolean,
): BaseViewItem<StringDesc>(name, Type) {
    companion object {
        const val Type = 402
    }
}