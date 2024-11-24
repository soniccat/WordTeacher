package com.aglushkov.wordteacher.android_app.helper

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import com.aglushkov.wordteacher.shared.general.auth.NetworkAuthData
import com.aglushkov.wordteacher.shared.general.auth.YandexAuthController
import com.aglushkov.wordteacher.shared.general.auth.YandexAuthData
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoadedOrError
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.yandex.authsdk.YandexAuthLoginOptions
import com.yandex.authsdk.YandexAuthOptions
import com.yandex.authsdk.YandexAuthResult
import com.yandex.authsdk.YandexAuthSdk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update

class YandexAuthControllerImpl : YandexAuthController {
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
                is YandexAuthResult.Success ->
                    yandexAuthDataState.value = Resource.Loaded(
                        YandexAuthData(
                            token = result.token.value,
                            expireTime = result.token.expiresIn,
                        )
                    )
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
