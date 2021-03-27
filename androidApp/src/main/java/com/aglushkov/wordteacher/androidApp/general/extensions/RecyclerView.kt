package com.aglushkov.wordteacher.androidApp.general.extensions

import androidx.recyclerview.widget.RecyclerView
import com.aglushkov.wordteacher.androidApp.general.SimpleAdapter
import com.aglushkov.wordteacher.androidApp.general.ViewItemBinder
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource

fun RecyclerView.submit(resource: Resource<List<BaseViewItem<*>>>, binder: ViewItemBinder) {
    if (adapter != null) {
        (adapter as SimpleAdapter).submitList(resource.data())
    } else {
        adapter = SimpleAdapter(binder).apply {
            submitList(resource.data())
        }
    }
}
