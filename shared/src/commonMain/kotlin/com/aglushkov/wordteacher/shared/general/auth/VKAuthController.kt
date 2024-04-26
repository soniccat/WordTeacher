package com.aglushkov.wordteacher.shared.general.auth

import com.aglushkov.wordteacher.shared.general.auth.NetworkAuthController
import com.aglushkov.wordteacher.shared.general.auth.NetworkAuthData
import com.aglushkov.wordteacher.shared.general.resource.Resource
import kotlinx.coroutines.flow.StateFlow

data class VKAuthData(
    override val token: String,
    val userID: Long,
    val expireTime: Long,
    val firstName: String,
    val lastName: String,
) : NetworkAuthData

interface VKAuthController: NetworkAuthController {
}