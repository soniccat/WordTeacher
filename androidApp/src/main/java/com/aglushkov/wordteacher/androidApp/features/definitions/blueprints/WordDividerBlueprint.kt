package com.aglushkov.wordteacher.androidApp.features.definitions.blueprints

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.features.Design
import com.aglushkov.wordteacher.androidApp.general.Blueprint
import com.aglushkov.wordteacher.androidApp.general.SimpleAdapter
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveThemeDrawable
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordDividerViewItem
import javax.inject.Inject

class WordDividerBlueprint @Inject constructor(): Blueprint<SimpleAdapter.ViewHolder<View>, WordDividerViewItem> {
    override val type: Int = WordDividerViewItem.Type

    override fun createViewHolder(parent: ViewGroup) = SimpleAdapter.ViewHolder(
        View(parent.context).apply {
            background = context.resolveThemeDrawable(R.attr.dividerHorizontal)
            Design.setTextHorizontalPadding(this)
        }
    )

    override fun bind(viewHolder: SimpleAdapter.ViewHolder<View>, viewItem: WordDividerViewItem) {
        val context = viewHolder.itemView.context
        val lp = viewHolder.itemView.layoutParams as RecyclerView.LayoutParams

        lp.height = context.resources.getDimensionPixelSize(R.dimen.word_divider_height)
        lp.topMargin = context.resources.getDimensionPixelSize(R.dimen.word_divider_topMargin)
        lp.bottomMargin = context.resources.getDimensionPixelSize(R.dimen.word_divider_bottomMargin)
    }
}