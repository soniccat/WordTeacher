package com.aglushkov.wordteacher.shared.general.crypto

import io.ktor.util.encodeBase64

fun ByteArray.encodeBase64Url() =
    this.encodeBase64().trimEnd('=').replace('+', '-').replace('/', '_')