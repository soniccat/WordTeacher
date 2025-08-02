package com.aglushkov.wordteacher.shared.general.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

enum class HintType {
    Introduction,
    DashboardArticles,
    DashboardCardSets,
    DashboardUsersArticles,
    DashboardUsersCardSets,
    Articles,
    AddArticle,
    Article,
    CardSets;

    override fun toString() = this.ordinal.toString()
}

fun Preferences.isHintClosed(hintType: HintType): Boolean {
    return boolean(hintName(hintType), false)
}

fun SettingStore.isHintClosed(hintType: HintType): Boolean {
    return boolean(hintName(hintType), false)
}

fun SettingStore.setHintClosed(hintType: HintType) {
    this[hintName(hintType)] = true
}

fun DataStore<Preferences>.setHintClosed(hintType: HintType) {
    this[hintName(hintType)] = true
}

fun hintName(hintType: HintType) = "hint_$hintType"
