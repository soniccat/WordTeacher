package com.aglushkov.wordteacher.androidApp.features.definitions.blueprints

import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.features.Design
import com.aglushkov.wordteacher.androidApp.general.Blueprint
import com.aglushkov.wordteacher.androidApp.general.SimpleAdapter
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveThemeStyle
import com.aglushkov.wordteacher.androidApp.general.extensions.setTextAppearanceCompat
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordHeaderViewItem
import javax.inject.Inject

class WordHeaderBlueprint @Inject constructor(): Blueprint<SimpleAdapter.ViewHolder<TextView>, WordHeaderViewItem> {
    override val type: Int = WordHeaderViewItem.Type

    override fun createViewHolder(parent: ViewGroup) = SimpleAdapter.ViewHolder(
        Design.createTextView(parent).apply {
            setTextAppearanceCompat(parent.context.resolveThemeStyle(R.attr.wordHeaderTextAppearance))
        }
    )

    override fun bind(viewHolder: SimpleAdapter.ViewHolder<TextView>, viewItem: WordHeaderViewItem) {
        val view = viewHolder.itemView
        viewHolder.typedView.text = viewItem.firstItem().toString(view.context)
        viewHolder.typedView.typeface = Typeface.DEFAULT_BOLD

        val lp = view.layoutParams as RecyclerView.LayoutParams
        lp.topMargin = view.resources.getDimensionPixelSize(R.dimen.word_header_topMargin)
    }
}