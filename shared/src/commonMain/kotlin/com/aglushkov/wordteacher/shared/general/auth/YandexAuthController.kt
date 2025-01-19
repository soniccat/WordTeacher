package com.aglushkov.wordteacher.shared.general.auth

data class YandexAuthData(
    override val token: String,
    val expireTime: Long,
) : NetworkAuthData {
    override fun isExpired(time: Long): Boolean =
        time > expireTime
}

interface YandexAuthController: NetworkAuthController {
}