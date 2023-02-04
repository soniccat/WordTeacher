package com.aglushkov.wordteacher.shared.general

import com.aglushkov.wordteacher.shared.general.resource.Resource
import kotlinx.coroutines.flow.StateFlow

data class GoogleAuthData(val name: String?, val tokenId: String?, val isSilent: Boolean)

interface GoogleAuthRepository {
    var googleSignInCredentialFlow: StateFlow<Resource<GoogleAuthData>>

    fun signIn()
}