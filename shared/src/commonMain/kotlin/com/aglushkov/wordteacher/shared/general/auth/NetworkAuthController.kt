package com.aglushkov.wordteacher.shared.general.auth

import com.aglushkov.wordteacher.shared.general.resource.Resource
import kotlinx.coroutines.flow.StateFlow

interface NetworkAuthData {
    val token: String
}

interface NetworkAuthController {
    var authDataFlow: StateFlow<Resource<NetworkAuthData>>

    suspend fun signIn(): Resource<NetworkAuthData>

    fun launchSignOut()
}
