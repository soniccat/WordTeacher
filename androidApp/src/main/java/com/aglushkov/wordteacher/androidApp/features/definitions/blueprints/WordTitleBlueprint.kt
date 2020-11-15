package com.aglushkov.wordteacher.androidApp.features.definitions.blueprints

import android.view.ViewGroup
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.features.definitions.views.WordTitleView
import com.aglushkov.wordteacher.androidApp.general.Blueprint
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordTitleViewItem
import javax.inject.Inject

class WordTitleBlueprint @Inject constructor(): Blueprint<WordTitleView, WordTitleViewItem> {
    override val type: Int = WordTitleViewItem.Type

    override fun createView(parent: ViewGroup): WordTitleView {
        return WordTitleView(parent.context)
    }

    override fun bind(view: WordTitleView, viewItem: WordTitleViewItem) {
        view.title.text = viewItem.firstItem()
        view.providedBy.text = view.context.getString(R.string.word_providedBy_template, viewItem.providers.joinToString())
    }
}