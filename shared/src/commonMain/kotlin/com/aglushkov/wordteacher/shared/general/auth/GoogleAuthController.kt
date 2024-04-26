package com.aglushkov.wordteacher.shared.general.auth

import com.aglushkov.wordteacher.shared.general.auth.NetworkAuthController
import com.aglushkov.wordteacher.shared.general.auth.NetworkAuthData
import com.aglushkov.wordteacher.shared.general.resource.Resource
import kotlinx.coroutines.flow.StateFlow

data class GoogleAuthData(val name: String?, val tokenId: String, val isSilent: Boolean): NetworkAuthData {
    override val token: String
        get() = tokenId
}

interface GoogleAuthController: NetworkAuthController {
}