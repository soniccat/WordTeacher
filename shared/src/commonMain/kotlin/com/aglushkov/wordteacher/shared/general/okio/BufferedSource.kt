package com.aglushkov.wordteacher.shared.general.okio

import okio.BufferedSource
import okio.utf8Size

fun BufferedSource.skipSpace() = skip(spaceSize)
fun BufferedSource.skipNewLine() = skip(newLineSize)

val spaceSize = " ".utf8Size()
val newLineSize = "\n".utf8Size()
