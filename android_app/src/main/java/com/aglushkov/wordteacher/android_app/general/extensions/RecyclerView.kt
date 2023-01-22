package com.aglushkov.wordteacher.android_app.general.extensions

import androidx.recyclerview.widget.RecyclerView
import com.aglushkov.wordteacher.android_app.general.SimpleAdapter
import com.aglushkov.wordteacher.android_app.general.ViewItemBinder
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
