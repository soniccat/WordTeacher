package com.aglushkov.wordteacher.androidApp.features.articles.blueprints

import android.content.Context
import android.view.View
import com.aglushkov.wordteacher.androidApp.databinding.ItemArticleBinding

class ArticleItemView(context: Context) : View(context) {
    lateinit var binding: ItemArticleBinding

    fun bind(title: String, date: String) {
        binding.title.text = title
        binding.date.text = date
    }
}