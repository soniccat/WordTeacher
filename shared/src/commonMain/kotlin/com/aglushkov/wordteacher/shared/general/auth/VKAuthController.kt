package com.aglushkov.wordteacher.shared.general.auth

import com.aglushkov.wordteacher.shared.general.auth.NetworkAuthController
import com.aglushkov.wordteacher.shared.general.auth.NetworkAuthData
import com.aglushkov.wordteacher.shared.general.resource.Resource
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.exp

data class VKAuthData(
    override val token: String,
    val userID: Long,
    val expireTime: Long,
) : NetworkAuthData {
    override fun isExpired(time: Long): Boolean =
        time >= expireTime

}

interface VKAuthController: NetworkAuthController {
}