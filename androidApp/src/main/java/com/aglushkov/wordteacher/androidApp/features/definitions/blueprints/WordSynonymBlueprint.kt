package com.aglushkov.wordteacher.androidApp.features.definitions.blueprints

import android.view.ViewGroup
import android.widget.TextView
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.features.Design
import com.aglushkov.wordteacher.androidApp.general.Blueprint
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveThemeStyle
import com.aglushkov.wordteacher.androidApp.general.extensions.setTextAppearanceCompat
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordSynonymViewItem

class WordSynonymBlueprint: Blueprint<TextView, WordSynonymViewItem> {
    override val type: Int = WordSynonymViewItem.Type

    override fun createView(parent: ViewGroup): TextView {
        return Design.createTextView(parent).apply {
            setTextAppearanceCompat(parent.context.resolveThemeStyle(R.attr.wordDefinitionTextAppearance))
        }
    }

    override fun bind(view: TextView, viewItem: WordSynonymViewItem) {
        view.text = viewItem.firstItem()
    }
}