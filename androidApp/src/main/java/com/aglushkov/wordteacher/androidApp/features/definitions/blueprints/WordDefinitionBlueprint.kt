package com.aglushkov.wordteacher.androidApp.features.definitions.blueprints

import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.features.Design
import com.aglushkov.wordteacher.androidApp.general.Blueprint
import com.aglushkov.wordteacher.androidApp.general.SimpleAdapter
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveThemeStyle
import com.aglushkov.wordteacher.androidApp.general.extensions.setTextAppearanceCompat
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordDefinitionViewItem
import javax.inject.Inject

class WordDefinitionBlueprint @Inject constructor(): Blueprint<SimpleAdapter.ViewHolder<TextView>, WordDefinitionViewItem> {
    override val type: Int = WordDefinitionViewItem.Type

    override fun createViewHolder(parent: ViewGroup) = SimpleAdapter.ViewHolder(
        Design.createTextView(parent).apply {
            setTextAppearanceCompat(parent.context.resolveThemeStyle(R.attr.wordDefinitionTextAppearance))
        }
    )

    override fun bind(viewHolder: SimpleAdapter.ViewHolder<TextView>, viewItem: WordDefinitionViewItem) {
        viewHolder.typedView.text = viewItem.firstItem()

        val lp = viewHolder.typedView.layoutParams as RecyclerView.LayoutParams
        lp.topMargin = viewHolder.itemView.resources.getDimensionPixelSize(R.dimen.word_header_topMargin)
    }
}