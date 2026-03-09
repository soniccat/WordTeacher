package com.aglushkov.wordteacher.shared.general.extensions

fun String.trimNonLetterNonDigit() = trim {
    !it.isLetterOrDigit()
}

fun String.replaceFirstToCapital() = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase() else it.toString()
}