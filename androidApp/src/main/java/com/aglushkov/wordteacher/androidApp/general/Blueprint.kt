package com.aglushkov.wordteacher.androidApp.general

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

interface Blueprint<H: RecyclerView.ViewHolder, I> {
    val type: Int

    fun createViewHolder(parent: ViewGroup): H
    fun bind(viewHolder: H, viewItem: I)
}
