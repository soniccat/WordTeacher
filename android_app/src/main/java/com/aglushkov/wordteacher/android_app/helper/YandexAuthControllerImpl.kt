package com.aglushkov.wordteacher.android_app.helper

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import com.aglushkov.wordteacher.shared.general.auth.NetworkAuthData
import com.aglushkov.wordteacher.shared.general.auth.YandexAuthController
import com.aglushkov.wordteacher.shared.general.auth.YandexAuthData
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoadedOrError
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.aglushkov.wordteacher.shared.general.resource.tryInResource
import com.yandex.authsdk.YandexAuthLoginOptions
import com.yandex.authsdk.YandexAuthOptions
import com.yandex.authsdk.YandexAuthResult
import com.yandex.authsdk.YandexAuthSdk
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class YandexAuthControllerImpl : YandexAuthController {
    private var scope = CoroutineScope(Dispatchers.Main)
    private var yandexSdk: YandexAuthSdk? = null
    private var yandexAuthDataState: MutableStateFlow<Resource<NetworkAuthData>> =
        MutableStateFlow(Resource.Uninitialized())
    override var authDataFlow: StateFlow<Resource<NetworkAuthData>> = yandexAuthDataState
    private var signInLauncher: ActivityResultLauncher<YandexAuthLoginOptions>? = null


    fun bind(activity: ComponentActivity) {
        val sdk = YandexAuthSdk.create(YandexAuthOptions(activity))
        yandexSdk = sdk
        signInLauncher = activity.registerForActivityResult(sdk.contract) { result ->
            when (result) {
                is YandexAuthResult.Success -> {
                    scope.launch(Dispatchers.Default) {
                        try {
                            yandexAuthDataState.value = Resource.Loaded(
                                YandexAuthData(
                                    token = sdk.getJwt(result.token),
                                    expireTime = result.token.expiresIn,
                                )
                            )
                        } catch (t: Throwable) {
                            yandexAuthDataState.value = Resource.Error(t, canTryAgain = true)
                        }
                    }
                }
                is YandexAuthResult.Failure ->
                    yandexAuthDataState.value = Resource.Error(result.exception, canTryAgain = true)
                is YandexAuthResult.Cancelled ->
                    yandexAuthDataState.value = Resource.Uninitialized()
            }
        }
    }

    override suspend fun signIn(): Resource<NetworkAuthData> {
        launchSignIn()
        yandexAuthDataState.takeWhile { it.isLoading() }.collect()
        return yandexAuthDataState.value
    }

    private suspend fun launchSignIn() {
        if (yandexAuthDataState.value.isLoading()) {
            return
        }
        yandexAuthDataState.value = Resource.Loading()
        signInLauncher?.launch(YandexAuthLoginOptions())
    }

    override fun launchSignOut() {
        yandexAuthDataState.update { Resource.Uninitialized() }
    }
}
