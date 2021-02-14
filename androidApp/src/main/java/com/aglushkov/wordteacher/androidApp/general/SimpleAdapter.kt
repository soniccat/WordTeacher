package com.aglushkov.wordteacher.androidApp.general

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aglushkov.wordteacher.androidApp.general.extensions.DiffCallback
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem

class SimpleAdapter(
    private val binder: ViewItemBinder
): ListAdapter<BaseViewItem<*>, RecyclerView.ViewHolder>(BaseViewItem.DiffCallback) {

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val blueprint = binder.findBlueprint(viewType) ?: throw IllegalStateException("Unexpected viewType: $viewType")
        return blueprint.createViewHolder(parent).apply {
            itemView.layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        val type = item.type
        val blueprint = binder.findBlueprint(type)!! as Blueprint<RecyclerView.ViewHolder, Any>
        blueprint.bind(holder, item)
    }

    class ViewHolder<T: View>(view: T): RecyclerView.ViewHolder(view) {
        val typedView: T
            get() = itemView as T
    }
}