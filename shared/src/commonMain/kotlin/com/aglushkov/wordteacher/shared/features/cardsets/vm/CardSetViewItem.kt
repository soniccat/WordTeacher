package com.aglushkov.wordteacher.shared.features.cardsets.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.StringResourceWrapper
import org.jetbrains.compose.resources.StringResource

class CardSetViewItem(
    val cardSetId: Long,
    val name: String,
    val date: String,
    val readyToLearnProgress: Float = 0f,
    val totalProgress: Float = 0f,
    val terms: List<String> = emptyList()
): BaseViewItem<Long>(cardSetId, Type) {
    companion object {
        const val Type = 400
    }

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        return super.equalsByContent(other) &&
                other is CardSetViewItem &&
                name == other.name &&
                date == other.date &&
                readyToLearnProgress == other.readyToLearnProgress &&
                totalProgress == other.totalProgress
    }
}

class CardSetExpandOrCollapseViewItem(
    val isExpanded: Boolean,
    val text: StringResourceWrapper,
): BaseViewItem<Boolean>(isExpanded, Type) {
    companion object {
        const val Type = 402
    }
}

class RemoteCardSetViewItem(
    val remoteCardSetId: String,
    val name: String,
    val terms: List<String>,
    val isLoading: Boolean,
    val isRead: Boolean = false,
): BaseViewItem<String>(name, Type) {
    companion object {
        const val Type = 401
    }

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        return super.equalsByContent(other) &&
                other is RemoteCardSetViewItem &&
                terms == other.terms &&
                isRead == other.isRead
    }
}

class SectionViewItem(
    val name: StringResourceWrapper,
    val isTop: Boolean,
): BaseViewItem<StringResourceWrapper>(name, Type) {
    companion object {
        const val Type = 402
    }
}