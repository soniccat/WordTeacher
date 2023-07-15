package com.aglushkov.wordteacher.shared.general.crypto

import java.security.MessageDigest


actual fun ByteArray.sha256(): ByteArray {
    val messageDigest: MessageDigest = MessageDigest.getInstance("SHA-256")
    messageDigest.update(this, 0, this.size)
    return messageDigest.digest()
}
