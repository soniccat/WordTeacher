package com.aglushkov.wordteacher.androidApp.features.definitions.blueprints

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.features.definitions.views.WordTitleView
import com.aglushkov.wordteacher.androidApp.general.Blueprint
import com.aglushkov.wordteacher.androidApp.general.SimpleAdapter
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordTitleViewItem
import javax.inject.Inject

class WordTitleBlueprint @Inject constructor(): Blueprint<SimpleAdapter.ViewHolder<WordTitleView>, WordTitleViewItem> {
    override val type: Int = WordTitleViewItem.Type

    override fun createViewHolder(parent: ViewGroup) = SimpleAdapter.ViewHolder(
        WordTitleView(parent.context)
    )

    override fun bind(viewHolder: SimpleAdapter.ViewHolder<WordTitleView>, viewItem: WordTitleViewItem) {
        val providedByString = viewHolder.itemView.context.getString(R.string.word_providedBy_template, viewItem.providers.joinToString())

        viewHolder.typedView.title.text = viewItem.firstItem()
        viewHolder.typedView.providedBy.text = providedByString
    }
}