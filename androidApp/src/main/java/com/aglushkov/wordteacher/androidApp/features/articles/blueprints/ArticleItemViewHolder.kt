package com.aglushkov.wordteacher.androidApp.features.articles.blueprints

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.aglushkov.wordteacher.androidApp.databinding.ItemArticleBinding

class ArticleItemViewHolder(
    private var binding: ItemArticleBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(title: String, date: String) {
        binding.title.text = title
        binding.date.text = date
    }
}