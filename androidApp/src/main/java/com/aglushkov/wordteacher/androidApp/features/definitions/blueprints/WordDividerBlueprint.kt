package com.aglushkov.wordteacher.androidApp.features.definitions.blueprints

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.features.Design
import com.aglushkov.wordteacher.androidApp.general.Blueprint
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveThemeDrawable
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordDividerViewItem

class WordDividerBlueprint: Blueprint<View, WordDividerViewItem> {
    override val type: Int = WordDividerViewItem.Type

    override fun createView(parent: ViewGroup): View {
        val context = parent.context
        val view = View(context)
        view.background = context.resolveThemeDrawable(R.attr.dividerHorizontal)
        Design.setTextHorizontalPadding(view)

        return view
    }

    override fun bind(view: View, viewItem: WordDividerViewItem) {
        val context = view.context
        val lp = view.layoutParams as RecyclerView.LayoutParams

        lp.height = context.resources.getDimensionPixelSize(R.dimen.word_divider_height)
        lp.topMargin = context.resources.getDimensionPixelSize(R.dimen.word_divider_topMargin)
        lp.bottomMargin = context.resources.getDimensionPixelSize(R.dimen.word_divider_bottomMargin)
    }
}