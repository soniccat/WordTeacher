package com.aglushkov.wordteacher.shared.general.extensions

inline fun <T: Collection<*>> T.runIfNotEmpty(func: (it: T) -> Unit) {
    if (this.isNotEmpty()) {
        func(this)
    }
}

fun <T> List<T>?.merge(collection: List<T>?) : List<T>? {
    if (this == null && collection == null) return null
    if (this != null && collection == null) return this
    if (this == null && collection != null) return collection
    return this!! + collection!!
}

fun <T> MutableList<T>.addElements(vararg elements: T) {
    addAll(elements)
}