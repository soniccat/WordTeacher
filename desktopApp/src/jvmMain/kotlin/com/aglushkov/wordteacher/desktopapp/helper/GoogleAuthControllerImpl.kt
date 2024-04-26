package com.aglushkov.wordteacher.desktopapp.helper

import com.aglushkov.wordteacher.shared.features.AuthOpener
import com.aglushkov.wordteacher.shared.features.Cancelled
import com.aglushkov.wordteacher.shared.general.auth.GoogleAuthController
import com.aglushkov.wordteacher.shared.general.auth.NetworkAuthData
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

    private var authDataState: MutableStateFlow<Resource<NetworkAuthData>> = MutableStateFlow(Resource.Uninitialized())
    override var authDataFlow: StateFlow<Resource<NetworkAuthData>> = authDataState

    override suspend fun signIn(): Resource<NetworkAuthData> {
        launchSignIn()
        authDataState.takeWhile { !it.isLoadedOrError() }.collect()
        return authDataState.value
    }

    private fun launchSignIn() {
        if (authDataState.value.isLoading()) {
            return
        }
        authDataState.value = Resource.Loading()

        authOpener?.openWebAuth(networkType = SpaceAuthService.NetworkType.Google)
    }

    override fun launchSignOut() {
        val prevValue = authDataState.value
        authDataState.value = Resource.Uninitialized()
    }

    // AuthOpener.Listener

    override fun onCompleted(result: AuthOpener.AuthResult) {
        result as AuthOpener.AuthResult.GoogleResult
        authDataState.value = Resource.Loaded(result.data)
    }

    override fun onError(throwable: Throwable) {
        if (throwable is Cancelled) {
            authDataState.value = Resource.Uninitialized()
        } else {
            authDataState.value = Resource.Error(throwable, canTryAgain = true)
        }
    }
}
