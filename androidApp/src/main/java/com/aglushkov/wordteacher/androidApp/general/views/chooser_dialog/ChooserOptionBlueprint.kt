package com.aglushkov.wordteacher.androidApp.general.views.chooser_dialog

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.databinding.ItemArticleBinding
import com.aglushkov.wordteacher.androidApp.databinding.ItemChooserBinding
import com.aglushkov.wordteacher.androidApp.features.Design
import com.aglushkov.wordteacher.androidApp.features.articles.blueprints.ArticleItemViewHolder
import com.aglushkov.wordteacher.androidApp.general.Blueprint
import com.aglushkov.wordteacher.androidApp.general.SimpleAdapter
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveThemeStyle
import com.aglushkov.wordteacher.androidApp.general.extensions.setTextAppearanceCompat
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordDefinitionViewItem
import javax.inject.Inject

class ChooserOptionBlueprint @Inject constructor(
    val onItemSelectionChanged: (viewItem: ChooserViewItem) -> Unit
): Blueprint<ChooserOptionViewHolder, ChooserViewItem> {
    override val type: Int = ChooserViewItem.Type

    override fun createViewHolder(parent: ViewGroup) = ChooserOptionViewHolder(
            ItemChooserBinding.inflate(LayoutInflater.from(parent.context))
        )

    override fun bind(viewHolder: ChooserOptionViewHolder, viewItem: ChooserViewItem) {
        viewHolder.itemView.setOnClickListener {
            onItemSelectionChanged(viewItem)
        }

        bindInternal(viewHolder, viewItem)
    }

    private fun bindInternal(
        viewHolder: ChooserOptionViewHolder,
        viewItem: ChooserViewItem
    ) {
        viewHolder.bind(viewItem.name, viewItem.isSelected)
    }
}

class ChooserOptionViewHolder(
    private var binding: ItemChooserBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(name: String, isChecked: Boolean) {
        binding.name.text = name
        binding.checkbox.isChecked = isChecked
    }
}