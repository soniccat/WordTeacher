package com.aglushkov.wordteacher.shared.general.extensions

fun String.trimNonLetterNonDigit() = trim {
    !it.isLetterOrDigit()
}

fun String.replaceFirstToCapital() = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase() else it.toString()
}

fun String.replaceFirstToLowerCase() = replaceFirstChar {
    if (!it.isLowerCase()) it.lowercase() else it.toString()
}

fun String.unbreakable() = replace(' ', '\u00A0')
    .replace('-', '\u2011')