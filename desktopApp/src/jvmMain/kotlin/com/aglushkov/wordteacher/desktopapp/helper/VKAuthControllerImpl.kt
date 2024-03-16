package com.aglushkov.wordteacher.desktopapp.helper

import com.aglushkov.wordteacher.shared.general.VKAuthController
import com.aglushkov.wordteacher.shared.general.auth.NetworkAuthData
import com.aglushkov.wordteacher.shared.general.resource.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class VKAuthControllerImpl : VKAuthController {
    private var vkAuthDataState: MutableStateFlow<Resource<NetworkAuthData>> =
        MutableStateFlow(Resource.Uninitialized())
    override var authDataFlow: StateFlow<Resource<NetworkAuthData>> = vkAuthDataState

    override suspend fun signIn(): Resource<NetworkAuthData> {
        TODO("Not implemented for desktop")
    }

    private suspend fun launchSignIn() {
        TODO("Not implemented for desktop")
    }

    override fun launchSignOut() {
        TODO("Not implemented for desktop")
    }
}
