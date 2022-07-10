package com.aglushkov.wordteacher.shared.general

fun Boolean.toLong(): Long =
    if (this) {
        1L
    } else {
        0L
    }
