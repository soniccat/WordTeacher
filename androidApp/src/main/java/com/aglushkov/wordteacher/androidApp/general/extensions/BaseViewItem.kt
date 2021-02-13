package com.aglushkov.wordteacher.androidApp.general.extensions

import androidx.recyclerview.widget.DiffUtil
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem

val BaseViewItem.Companion.DiffCallback: DiffUtil.ItemCallback<BaseViewItem<*>>
    get() {
        return object : DiffUtil.ItemCallback<BaseViewItem<*>>() {
            override fun areItemsTheSame(oldCellInfo: BaseViewItem<*>, newCellInfo: BaseViewItem<*>): Boolean {
                return oldCellInfo.equalsByIds(newCellInfo)
            }

            override fun areContentsTheSame(oldCellInfo: BaseViewItem<*>, newCellInfo: BaseViewItem<*>): Boolean {
                return oldCellInfo.equalsByContent(newCellInfo)
            }

            override fun getChangePayload(
                oldItem: BaseViewItem<*>,
                newItem: BaseViewItem<*>
            ): Any {
                //return super.getChangePayload(oldItem, newItem)
                return Unit
            }
        }
    }