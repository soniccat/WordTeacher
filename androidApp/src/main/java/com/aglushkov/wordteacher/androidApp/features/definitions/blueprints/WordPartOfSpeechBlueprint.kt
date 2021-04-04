package com.aglushkov.wordteacher.androidApp.features.definitions.blueprints

import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.features.Design
import com.aglushkov.wordteacher.androidApp.general.Blueprint
import com.aglushkov.wordteacher.androidApp.general.SimpleAdapter
import com.aglushkov.wordteacher.androidApp.general.extensions.getColorCompat
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveThemeStyle
import com.aglushkov.wordteacher.androidApp.general.extensions.setTextAppearanceCompat
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordPartOfSpeechViewItem
import java.util.*
import javax.inject.Inject

class WordPartOfSpeechBlueprint @Inject constructor(): Blueprint<SimpleAdapter.ViewHolder<TextView>, WordPartOfSpeechViewItem> {
    override val type: Int = WordPartOfSpeechViewItem.Type

    override fun createViewHolder(parent: ViewGroup) = SimpleAdapter.ViewHolder(
        Design.createTextView(parent).apply {
            setTextAppearanceCompat(parent.context.resolveThemeStyle(R.attr.wordPartOfSpeechTextAppearance))
        }
    )

    override fun bind(viewHolder: SimpleAdapter.ViewHolder<TextView>, viewItem: WordPartOfSpeechViewItem) {
        val view = viewHolder.typedView
        viewHolder.typedView.setTextColor(view.context.getColorCompat(R.color.word_partOfSpeech))
        viewHolder.typedView.text = viewItem.firstItem().toString(context = view.context).toUpperCase(
            Locale.getDefault())

        val lp = view.layoutParams as RecyclerView.LayoutParams
        lp.topMargin = view.resources.getDimensionPixelSize(R.dimen.word_partOfSpeech_topMargin)
    }
}
