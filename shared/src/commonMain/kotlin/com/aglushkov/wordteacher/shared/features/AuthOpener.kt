package com.aglushkov.wordteacher.shared.features

import com.aglushkov.wordteacher.shared.general.GoogleAuthData
import com.aglushkov.wordteacher.shared.service.SpaceAuthService

interface AuthOpener {
    fun addAuthListener(listener: Listener)
    fun removeAuthListener(listener: Listener)
    fun openWebAuth(networkType: SpaceAuthService.NetworkType)

    interface Listener {
        fun onCompleted(result: AuthResult)
        fun onError(throwable: Throwable)
    }

    sealed interface AuthResult {
        data class GoogleResult(val data: GoogleAuthData): AuthResult
    }
}

object Cancelled: Throwable("Cancelled")