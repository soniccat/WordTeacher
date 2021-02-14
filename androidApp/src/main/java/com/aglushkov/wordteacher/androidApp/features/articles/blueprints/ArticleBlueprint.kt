package com.aglushkov.wordteacher.androidApp.features.articles.blueprints

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aglushkov.wordteacher.androidApp.databinding.ItemArticleBinding
import com.aglushkov.wordteacher.androidApp.general.Blueprint
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticleViewItem
import javax.inject.Inject

class ArticleBlueprint @Inject constructor(): Blueprint<ArticleItemViewHolder, ArticleViewItem> {
    override val type: Int = ArticleViewItem.Type

    override fun createViewHolder(parent: ViewGroup): ArticleItemViewHolder {
        val binding = ItemArticleBinding.inflate(LayoutInflater.from(parent.context))
        return ArticleItemViewHolder(binding)
    }

    override fun bind(viewHolder: ArticleItemViewHolder, viewItem: ArticleViewItem) {
        viewHolder.bind(
            viewItem.name,
            viewItem.date
        )
    }
}