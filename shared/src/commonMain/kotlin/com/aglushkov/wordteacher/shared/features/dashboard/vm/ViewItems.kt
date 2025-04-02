package com.aglushkov.wordteacher.shared.features.dashboard.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.datetime.Instant

class DashboardCategoriesViewItem(
    categories: List<String>,
    val selectedIndex: Int,
): BaseViewItem<String>(categories, Type) {
    companion object {
        const val Type = 1200
    }
}

class DashboardHeadlineViewItem(
    id: String,
    val title: String,
    val description: String?,
    val sourceName: String,
    val sourceCategory: String,
    val date: Instant,
    val link: String,
): BaseViewItem<String>(id, Type) {
    companion object {
        const val Type = 1201
    }
}

class DashboardTryAgainViewItem(
    val errorText: StringDesc,
    val tryAgainActionText: StringDesc,
): BaseViewItem<Unit>(Unit, Type) {
    companion object {
        const val Type = 1202
    }
}

class DashboardExpandViewItem(
    expandType: ExpandType,
    val isExpanded: Boolean,
): BaseViewItem<DashboardExpandViewItem.ExpandType>(expandType, Type) {
    enum class ExpandType {
        Headline,
        CardSets,
    }

    companion object {
        const val Type = 1203
    }
}