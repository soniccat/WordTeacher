package com.aglushkov.wordteacher.androidApp.features.definitions.blueprints

import android.view.ViewGroup
import android.widget.TextView
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.features.Design
import com.aglushkov.wordteacher.androidApp.general.Blueprint
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveThemeStyle
import com.aglushkov.wordteacher.androidApp.general.extensions.setTextAppearanceCompat
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordTranscriptionViewItem
import javax.inject.Inject

class WordTranscriptionBlueprint @Inject constructor(): Blueprint<TextView, WordTranscriptionViewItem> {
    override val type: Int = WordTranscriptionViewItem.Type

    override fun createView(parent: ViewGroup): TextView {
        return Design.createTextView(parent).apply {
            setTextAppearanceCompat(parent.context.resolveThemeStyle(R.attr.wordTranscriptionTextAppearance))
        }
    }

    override fun bind(view: TextView, viewItem: WordTranscriptionViewItem) {
        view.text = viewItem.firstItem()
    }
}