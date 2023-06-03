package com.aglushkov.wordteacher.desktopapp.helper

import com.aglushkov.wordteacher.shared.features.AuthOpener
import com.aglushkov.wordteacher.shared.features.Cancelled
import com.aglushkov.wordteacher.shared.general.GoogleAuthData
import com.aglushkov.wordteacher.shared.general.GoogleAuthController
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoadedOrError
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.aglushkov.wordteacher.shared.service.SpaceAuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile

class GoogleAuthControllerImpl(): GoogleAuthController, AuthOpener.Listener {
    var authOpener: AuthOpener? = null
        set(value) {
            field?.removeAuthListener(this)
            field = value
            field?.addAuthListener(this)
        }

    private var googleAuthDataState: MutableStateFlow<Resource<GoogleAuthData>> = MutableStateFlow(Resource.Uninitialized())
    override var googleAuthDataFlow: StateFlow<Resource<GoogleAuthData>> = googleAuthDataState

    override suspend fun signIn(): Resource<GoogleAuthData> {
        launchSignIn()
        googleAuthDataState.takeWhile { !it.isLoadedOrError() }.collect()
        return googleAuthDataState.value
    }

    override fun launchSignIn() {
        if (googleAuthDataState.value.isLoading()) {
            return
        }
        googleAuthDataState.value = Resource.Loading()

        authOpener?.openWebAuth(networkType = SpaceAuthService.NetworkType.Google)
    }

    override fun launchSignOut() {
        val prevValue = googleAuthDataState.value
    }

    // AuthOpener.Listener

    override fun onCompleted(result: AuthOpener.AuthResult) {
        TODO("Not yet implemented")
    }

    override fun onError(throwable: Throwable) {
        if (throwable is Cancelled) {
            googleAuthDataState.value = Resource.Uninitialized()
        } else {
            googleAuthDataState.value = Resource.Error(throwable, canTryAgain = true)
        }
    }
}
