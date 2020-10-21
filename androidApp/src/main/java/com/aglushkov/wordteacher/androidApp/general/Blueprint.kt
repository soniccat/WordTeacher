package com.aglushkov.wordteacher.androidApp.general

import android.view.ViewGroup

interface Blueprint<V, I> {
    val type: Int

    fun createView(parent: ViewGroup): V
    fun bind(view: V, viewItem: I)
}
