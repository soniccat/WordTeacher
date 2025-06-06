package com.aglushkov.wordteacher.shared.general.item

import androidx.compose.runtime.Stable
import com.aglushkov.wordteacher.shared.general.IdGenerator

@Stable
abstract class BaseViewItem<T> {
    var id = 0L // a unique id, required for NSDiffableDataSourceSnapshot and Compose LazyList key
    var type = 0
    var items = listOf<T>()

    constructor(item: T, type: Int, id: Long = 0L): this(listOf(item), type, id)

    constructor(items: List<T>, type: Int, id: Long = 0L) {
        assert(items.isNotEmpty())
        this.items = items
        this.type = type
        this.id = id
    }

    fun firstItem() = items.first()

    open fun equalsByIds(item: BaseViewItem<*>): Boolean {
        // by default we compare the content as we don't have ids here
        // if an id is available in a subclass this check should be used:
        //     this.javaClass == item.javaClass && type == item.type && id == item.id
        // and equalsByContent should be overridden too
        return this::class == item::class && type == item.type && id == item.id
    }

    open fun equalsByContent(other: BaseViewItem<*>): Boolean {
        // as we check the content in equalsByIds we can return true here
        // this should be overridden if equalsByIds is overridden
        return items == other.items
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BaseViewItem<*>) return false

        if (type != other.type) return false
        if (items != other.items) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type
        result = 31 * result + items.hashCode()
        result = 31 * result + id.toInt()
        return result
    }

    fun itemsHashCode(): Int = this.items.hashCode()
    fun itemsEquals(items: List<T>) = this.items.equals(items)

    companion object {}
}

// Set unique id taking into account that for the same items id shouldn't change
fun generateViewItemIds(
    items: List<BaseViewItem<*>>,
    prevItems: List<BaseViewItem<*>> = emptyList(),
    idGenerator: IdGenerator,
    onIdSet: (newItem: BaseViewItem<*>, oldItem: BaseViewItem<*>?) -> Unit = { a, b -> }
): List<BaseViewItem<*>> {
    val map: MutableMap<Int, MutableList<BaseViewItem<*>>> = mutableMapOf()

    // Put items with ids in map
    prevItems.forEach {
        val itemsHashCode = it.itemsHashCode()

        // Obtain mutable list
        val listOfViewItems: MutableList<BaseViewItem<*>> = map[itemsHashCode]
            ?: mutableListOf<BaseViewItem<*>>().also { list ->
                map[itemsHashCode] = list
            }

        listOfViewItems.add(it)
    }

    // set ids for item not in the map
    items.forEach {
        // TODO: refactor, probably we should check equality by ids first, before creating the map
        val equalByIdItem = prevItems.firstOrNull { prevItem ->
            prevItem.equalsByIds(it) && prevItem.equalsByContent(it)
        }

        if (equalByIdItem != null) {
            onIdSet(it, equalByIdItem)

        } else if (it.id != 0L) {
            onIdSet(it, null)

        } else {
            val itemsHashCode = it.itemsHashCode()
            val mapListOfViewItems = map[itemsHashCode]
            val item = mapListOfViewItems?.firstOrNull { listItem -> listItem.equalsByIds(it) && listItem.equalsByContent(it) }

            if (item != null) {
                it.id = item.id
                mapListOfViewItems.remove(item)
            } else {
                it.id = idGenerator.nextId()
            }
            onIdSet(it, item)
        }
    }

    return items
}
