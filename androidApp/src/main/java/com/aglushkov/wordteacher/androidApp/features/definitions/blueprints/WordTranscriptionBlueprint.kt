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
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordTranscriptionViewItem
import javax.inject.Inject

class WordTranscriptionBlueprint @Inject constructor(): Blueprint<SimpleAdapter.ViewHolder<TextView>, WordTranscriptionViewItem> {
    override val type: Int = WordTranscriptionViewItem.Type

    override fun createViewHolder(parent: ViewGroup) = SimpleAdapter.ViewHolder(
        Design.createTextView(parent).apply {
            setTextAppearanceCompat(parent.context.resolveThemeStyle(R.attr.wordTranscriptionTextAppearance))
        }
    )

    override fun bind(viewHolder: SimpleAdapter.ViewHolder<TextView>, viewItem: WordTranscriptionViewItem) {
        viewHolder.typedView.text = viewItem.firstItem()
    }
}