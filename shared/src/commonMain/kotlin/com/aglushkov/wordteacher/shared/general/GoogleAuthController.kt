package com.aglushkov.wordteacher.shared.general

import com.aglushkov.wordteacher.shared.general.resource.Resource
import kotlinx.coroutines.flow.StateFlow

data class GoogleAuthData(val name: String?, val tokenId: String, val isSilent: Boolean)

interface GoogleAuthController {
    var googleAuthDataFlow: StateFlow<Resource<GoogleAuthData>>

    fun launchSignIn()
    suspend fun signIn(): Resource<GoogleAuthData>

    fun launchSignOut()
}