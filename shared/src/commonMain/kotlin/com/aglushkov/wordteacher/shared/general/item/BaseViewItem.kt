package com.aglushkov.wordteacher.shared.general.item

abstract class BaseViewItem<T> {
    var type = 0
    var items = listOf<T>()

    constructor(item: T, type: Int) {
        this.items = listOf(item)
        this.type = type
    }

    constructor(items: List<T>, type: Int) {
        this.items = items
        this.type = type
    }

    fun firstItem() = items.first()

    open fun equalsByIds(item: BaseViewItem<*>): Boolean {
        // by default we compare the content as we don't have ids here
        // if an id is available in a subclass this check should be used:
        //     this.javaClass == item.javaClass && type == item.type && id == item.id
        // and equalsByContent should be overridden too
        return equals(item)
    }

    open fun equalsByContent(other: Any?): Boolean {
        // as we check the content in equalsByIds we can return true here
        // this should be overridden if equalsByIds is overridden
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BaseViewItem<*>) return false

        if (type != other.type) return false
        if (items != other.items) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type
        result = 31 * result + items.hashCode()
        return result
    }

//    companion object {
//        // ListAdapter diff callback
//        @JvmStatic val DiffCallback = object : DiffUtil.ItemCallback<BaseViewItem<*>>() {
//            override fun areItemsTheSame(oldCellInfo: BaseViewItem<*>, newCellInfo: BaseViewItem<*>): Boolean {
//                return oldCellInfo.equalsByIds(newCellInfo)
//            }
//
//            override fun areContentsTheSame(oldCellInfo: BaseViewItem<*>, newCellInfo: BaseViewItem<*>): Boolean {
//                return oldCellInfo.equalsByContent(newCellInfo)
//            }
//        }
//    }
}