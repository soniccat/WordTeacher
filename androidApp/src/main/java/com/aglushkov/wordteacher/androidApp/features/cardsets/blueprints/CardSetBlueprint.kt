package com.aglushkov.wordteacher.androidApp.features.cardsets.blueprints

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aglushkov.wordteacher.androidApp.databinding.ItemArticleBinding
import com.aglushkov.wordteacher.androidApp.features.articles.views.ArticlesAndroidVM
import com.aglushkov.wordteacher.androidApp.features.cardsets.views.CardSetsAndroidVM
import com.aglushkov.wordteacher.androidApp.general.Blueprint
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticleViewItem
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetViewItem
import javax.inject.Inject

class CardSetBlueprint @Inject constructor(
    val vm: CardSetsAndroidVM
): Blueprint<CardSetItemViewHolder, CardSetViewItem> {
    override val type: Int = ArticleViewItem.Type

    override fun createViewHolder(parent: ViewGroup) = CardSetItemViewHolder(
            ItemArticleBinding.inflate(LayoutInflater.from(parent.context))
        )

    override fun bind(viewHolder: CardSetItemViewHolder, viewItem: CardSetViewItem) {
        viewHolder.bind(
            viewItem.name,
            viewItem.date
        )

        viewHolder.itemView.setOnClickListener {
            vm.vm.onCardSetClicked(viewItem)
        }
    }
}

class CardSetItemViewHolder(
    private var binding: ItemArticleBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(title: String, date: String) {
        binding.title.text = title
        binding.date.text = date
    }
}