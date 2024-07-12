package com.aglushkov.wordteacher.shared.general

interface EmailOpener {
    fun open(email: String)
}