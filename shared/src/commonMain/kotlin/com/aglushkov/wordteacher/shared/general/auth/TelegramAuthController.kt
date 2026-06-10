package com.aglushkov.wordteacher.shared.general.auth

import java.net.URI

data class TelegramAuthData(
    override val token: String,
) : NetworkAuthData {
    override fun isExpired(time: Long): Boolean = true
}

interface TelegramAuthController: NetworkAuthController {
}