package com.aglushkov.wordteacher.desktopapp.helper

import com.aglushkov.wordteacher.shared.general.GoogleAuthData
import com.aglushkov.wordteacher.shared.general.GoogleAuthRepository
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoadedOrError
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile

class GoogleAuthRepositoryImpl(): GoogleAuthRepository {
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
    }


    override fun launchSignOut() {
        val prevValue = googleAuthDataState.value
    }
}
