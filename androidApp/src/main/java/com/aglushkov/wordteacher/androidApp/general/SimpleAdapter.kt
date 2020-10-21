package com.aglushkov.wordteacher.androidApp.general

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aglushkov.wordteacher.androidApp.general.extensions.DiffCallback
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem

interface Blueprint<V, I> {
    val type: Int

    fun createView(parent: ViewGroup): V
    fun bind(view: V, viewItem: I)
}

class ViewItemBinder {
    private val blueprints = mutableMapOf<Int, Blueprint<*,*>>()

    fun addBlueprint(blueprint: Blueprint<*,*>): ViewItemBinder {
        blueprints[blueprint.type] = blueprint
        return this
    }

    fun findBlueprint(type: Int): Blueprint<*,*>? {
        return blueprints[type]
    }
}

class SimpleAdapter(
    private val binder: ViewItemBinder
): ListAdapter<BaseViewItem<*>, SimpleAdapter.ViewHolder>(BaseViewItem.DiffCallback) {

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val blueprint = binder.findBlueprint(viewType) ?: throw IllegalStateException("Unexpected viewType: $viewType")
        val view = blueprint.createView(parent) as View
        view.layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val type = item.type
        val blueprint = binder.findBlueprint(type)!! as Blueprint<Any, Any>
        blueprint.bind(holder.itemView, item)
    }

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
    }
}