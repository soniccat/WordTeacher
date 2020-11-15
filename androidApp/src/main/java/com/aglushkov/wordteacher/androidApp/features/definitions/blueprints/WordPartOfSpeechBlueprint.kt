package com.aglushkov.wordteacher.androidApp.features.definitions.blueprints

import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.features.Design
import com.aglushkov.wordteacher.androidApp.general.Blueprint
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveThemeStyle
import com.aglushkov.wordteacher.androidApp.general.extensions.setTextAppearanceCompat
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordPartOfSpeechViewItem
import javax.inject.Inject

class WordPartOfSpeechBlueprint @Inject constructor(): Blueprint<TextView, WordPartOfSpeechViewItem> {
    override val type: Int = WordPartOfSpeechViewItem.Type

    override fun createView(parent: ViewGroup): TextView {
        return Design.createTextView(parent).apply {
            setTextAppearanceCompat(parent.context.resolveThemeStyle(R.attr.wordPartOfSpeechTextAppearance))
        }
    }

    override fun bind(view: TextView, viewItem: WordPartOfSpeechViewItem) {
        view.text = viewItem.firstItem().toString(context = view.context)

        val lp = view.layoutParams as RecyclerView.LayoutParams
        lp.topMargin = view.resources.getDimensionPixelSize(R.dimen.word_partOfSpeech_topMargin)
    }
}