package com.aglushkov.wordteacher.android_app.helper

import androidx.activity.ComponentActivity
import com.aglushkov.wordteacher.android_app.getTelegramRedirect
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.auth.NetworkAuthData
import com.aglushkov.wordteacher.shared.general.auth.TelegramAuthController
import com.aglushkov.wordteacher.shared.general.auth.TelegramAuthData
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import org.telegram.login.TelegramLogin

class TelegramAuthControllerImpl(
    val timeSource: TimeSource,
    val isDebug: Boolean,
): TelegramAuthController {
    private var activity: ComponentActivity? = null
    private var authDataState: MutableStateFlow<Resource<NetworkAuthData>> =
        MutableStateFlow(Resource.Uninitialized())
    override var authDataFlow: StateFlow<Resource<NetworkAuthData>> = authDataState

    fun bind(activity: ComponentActivity) {
        this.activity = activity
        activity.addOnNewIntentListener { it ->
            it.data?.let { uri ->
                if (uri.host == getTelegramRedirect(activity.resources, isDebug)) {
                    TelegramLogin.handleLoginResponse(
                        uri,
                        onSuccess = { loginData ->
                            authDataState.value = Resource.Loaded(
                                TelegramAuthData(token = loginData.idToken)
                            )
                        }, onError = { error ->
                            authDataState.value = Resource.Error(
                                RuntimeException("Login failed: ${error.message}"),
                                canTryAgain = true,
                            )
                        }
                    )
                }
            }
        }
    }

    override suspend fun signIn(): Resource<NetworkAuthData> {
        val safeActivity = activity ?: return Resource.Uninitialized()

        if (authDataState.value.isLoading()) {
            return authDataState.value
        }

        authDataState.value = Resource.Loading()
        TelegramLogin.startLogin(safeActivity)
        authDataState.takeWhile { it.isLoading() }.collect()

        return authDataState.value
    }

    override fun launchSignOut() {
        authDataState.update { Resource.Uninitialized() }
    }
}
