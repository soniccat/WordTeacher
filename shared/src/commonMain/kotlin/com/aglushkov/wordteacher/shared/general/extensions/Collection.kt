package com.aglushkov.wordteacher.shared.general.extensions

import kotlin.math.min

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

fun <T> List<T>.splitByChunks(chunkSize: Int): List<List<T>> {
    val resultList = mutableListOf<List<T>>()
    for (i in indices step chunkSize) {
        val endI = min(size, i + chunkSize)
        resultList.add(subList(i, endI))
    }
    return resultList
}

fun <T> List<T>.splitBy(splitter: (T) -> Boolean): Pair<List<T>, List<T>> {
    val firstList = mutableListOf<T>()
    val secondList = mutableListOf<T>()
    onEach {
        if (splitter(it)) {
            firstList.add(it)
        } else {
            firstList.add(it)
        }
    }
    return Pair(firstList, secondList)
}