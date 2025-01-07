package com.aglushkov.wordteacher.shared.features.cardset_info.vm

interface CardSetInfoRouter {
    fun openAddArticle(url: String, showNeedToCreateCardSet: Boolean)
    fun onClosed()
}