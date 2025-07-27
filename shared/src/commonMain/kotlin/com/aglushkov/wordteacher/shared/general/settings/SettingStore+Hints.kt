package com.aglushkov.wordteacher.shared.general.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.aglushkov.wordteacher.shared.features.dashboard.vm.HintViewItem

enum class HintType {
    HintIntroduction,
    DashboardArticles,
    DashboardCardSets,
    DashboardUsersArticles,
    DashboardUsersCardSets;

    override fun toString() = this.ordinal.toString()
}

fun Preferences.isHintShown(hintType: HintType): Boolean {
    return boolean(hintName(hintType), false)
}

fun SettingStore.setHintShown(hintType: HintType) {
    this[hintName(hintType)] = true
}

fun DataStore<Preferences>.setHintShown(hintType: HintType) {
    this[hintName(hintType)] = true
}

private fun hintName(hintType: HintType) = "hint_$hintType"
