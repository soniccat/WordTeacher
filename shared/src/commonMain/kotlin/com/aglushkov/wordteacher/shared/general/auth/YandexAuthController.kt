package com.aglushkov.wordteacher.shared.general.auth

data class YandexAuthData(
    override val token: String,
    val expireTime: Long,
) : NetworkAuthData

interface YandexAuthController: NetworkAuthController {
}