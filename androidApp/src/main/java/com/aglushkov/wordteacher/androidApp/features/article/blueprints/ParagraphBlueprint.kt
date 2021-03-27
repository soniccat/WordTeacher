package com.aglushkov.wordteacher.androidApp.features.article.blueprints

import android.view.ViewGroup
import android.widget.TextView
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.features.Design
import com.aglushkov.wordteacher.androidApp.general.Blueprint
import com.aglushkov.wordteacher.androidApp.general.SimpleAdapter
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveThemeStyle
import com.aglushkov.wordteacher.androidApp.general.extensions.setTextAppearanceCompat
import com.aglushkov.wordteacher.shared.features.article.vm.ParagraphViewItem
import javax.inject.Inject

class ParagraphBlueprint @Inject constructor(): Blueprint<SimpleAdapter.ViewHolder<TextView>, ParagraphViewItem> {
    override val type: Int = ParagraphViewItem.Type

    override fun createViewHolder(parent: ViewGroup) = SimpleAdapter.ViewHolder(
        Design.createTextView(parent).apply {
            setTextAppearanceCompat(parent.context.resolveThemeStyle(R.attr.wordDefinitionTextAppearance))
        }
    )

    override fun bind(viewHolder: SimpleAdapter.ViewHolder<TextView>, viewItem: ParagraphViewItem) {
        viewHolder.typedView.text = viewItem.items
            .map {
                it.text
            }
            .fold("") { a, b ->
                "$a\n$b"
            }
    }
}