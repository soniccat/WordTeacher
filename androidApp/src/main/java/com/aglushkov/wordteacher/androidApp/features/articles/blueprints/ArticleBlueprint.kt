package com.aglushkov.wordteacher.androidApp.features.articles.blueprints

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.databinding.ItemArticleBinding
import com.aglushkov.wordteacher.androidApp.features.Design
import com.aglushkov.wordteacher.androidApp.general.Blueprint
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveThemeStyle
import com.aglushkov.wordteacher.androidApp.general.extensions.setTextAppearanceCompat
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticleViewItem
import javax.inject.Inject

class ArticleBlueprint @Inject constructor(): Blueprint<ArticleItemView, ArticleViewItem> {
    override val type: Int = ArticleViewItem.Type

    override fun createView(parent: ViewGroup): ArticleItemView {
        return ItemArticleBinding.inflate(
            LayoutInflater.from(parent.context)
        )
    }

    override fun bind(view: ArticleItemView, viewItem: ArticleViewItem) {
        view.bind(
            viewItem.name,
            viewItem.date
        )
    }
}