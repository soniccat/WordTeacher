package com.aglushkov.wordteacher.androidApp.features.articles.blueprints

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aglushkov.wordteacher.androidApp.databinding.ItemArticleBinding
import com.aglushkov.wordteacher.androidApp.features.articles.views.ArticlesVMWrapper
import com.aglushkov.wordteacher.androidApp.general.Blueprint
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticleViewItem
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesVM
import javax.inject.Inject

class ArticleBlueprint @Inject constructor(
    val vm: ArticlesVMWrapper
): Blueprint<ArticleItemViewHolder, ArticleViewItem> {
    override val type: Int = ArticleViewItem.Type

    override fun createViewHolder(parent: ViewGroup) = ArticleItemViewHolder(
            ItemArticleBinding.inflate(LayoutInflater.from(parent.context))
        )

    override fun bind(viewHolder: ArticleItemViewHolder, viewItem: ArticleViewItem) {
        viewHolder.bind(
            viewItem.name,
            viewItem.date
        )

        viewHolder.itemView.setOnClickListener {
            vm.vm.onArticleClicked(viewItem)
        }
    }
}