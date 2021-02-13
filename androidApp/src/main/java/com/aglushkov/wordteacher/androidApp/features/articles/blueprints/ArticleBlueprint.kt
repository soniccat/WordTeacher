package com.aglushkov.wordteacher.androidApp.features.articles.blueprints

import android.view.ViewGroup
import android.widget.TextView
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.features.Design
import com.aglushkov.wordteacher.androidApp.general.Blueprint
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveThemeStyle
import com.aglushkov.wordteacher.androidApp.general.extensions.setTextAppearanceCompat
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticleViewItem
import javax.inject.Inject

class ArticleBlueprint @Inject constructor(): Blueprint<TextView, ArticleViewItem> {
    override val type: Int = ArticleViewItem.Type

    override fun createView(parent: ViewGroup): TextView {
        return Design.createTextView(parent).apply {
            setTextAppearanceCompat(parent.context.resolveThemeStyle(R.attr.wordExampleTextAppearance))
        }
    }

    override fun bind(view: TextView, viewItem: ArticleViewItem) {
        view.text = viewItem.firstItem()
    }
}