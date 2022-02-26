package com.aglushkov.wordteacher.shared.general.extensions

fun String.trimNonLetterNonDigit() = trim {
    !it.isLetterOrDigit()
}