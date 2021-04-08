package com.aglushkov.wordteacher.androidApp.features.articles.blueprints

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aglushkov.wordteacher.androidApp.databinding.ItemArticleBinding
import com.aglushkov.wordteacher.androidApp.features.articles.views.ArticlesAndroidVM
import com.aglushkov.wordteacher.androidApp.general.Blueprint
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticleViewItem
import javax.inject.Inject

class ArticleBlueprint @Inject constructor(
    val vm: ArticlesAndroidVM
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

class ArticleItemViewHolder(
    private var binding: ItemArticleBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(title: String, date: String) {
        binding.title.text = title
        binding.date.text = date
    }
}