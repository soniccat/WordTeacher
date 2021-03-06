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
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordSubHeaderViewItem
import javax.inject.Inject

class WordSubHeaderBlueprint @Inject constructor(): Blueprint<SimpleAdapter.ViewHolder<TextView>, WordSubHeaderViewItem> {
    override val type: Int = WordSubHeaderViewItem.Type

    override fun createViewHolder(parent: ViewGroup) = SimpleAdapter.ViewHolder(
        Design.createTextView(parent).apply {
            setTextAppearanceCompat(parent.context.resolveThemeStyle(R.attr.wordSubHeaderTextAppearance))
        }
    )

    override fun bind(viewHolder: SimpleAdapter.ViewHolder<TextView>, viewItem: WordSubHeaderViewItem) {
        val view = viewHolder.itemView
        viewHolder.typedView.setTextColor(view.context.getColorCompat(R.color.word_subheader))
        viewHolder.typedView.text = viewItem.firstItem().toString(view.context)

        val lp = view.layoutParams as RecyclerView.LayoutParams
        lp.leftMargin = viewItem.indent.toDp(view.resources)
        lp.topMargin = view.resources.getDimensionPixelSize(R.dimen.word_subHeader_topMargin)
    }
}