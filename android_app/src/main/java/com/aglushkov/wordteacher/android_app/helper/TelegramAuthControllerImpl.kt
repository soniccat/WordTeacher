package com.aglushkov.wordteacher.android_app.helper

import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.aglushkov.wordteacher.android_app.getTelegramRedirect
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.auth.NetworkAuthData
import com.aglushkov.wordteacher.shared.general.auth.TelegramAuthController
import com.aglushkov.wordteacher.shared.general.auth.TelegramAuthData
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.login.TelegramLogin
import java.lang.ref.WeakReference

class TelegramAuthControllerImpl(
    val timeSource: TimeSource,
    val isDebug: Boolean,
): TelegramAuthController {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var authJob: Job? = null
    private var cancelJob: Job? = null

    private var weakActivity: WeakReference<ComponentActivity>? = null
    private var authDataState: MutableStateFlow<Resource<NetworkAuthData>> =
        MutableStateFlow(Resource.Uninitialized())
    override var authDataFlow: StateFlow<Resource<NetworkAuthData>> = authDataState

    fun bind(activity: ComponentActivity) {
        this.weakActivity = WeakReference(activity)

        // treat coming back as cancellation after delay
        activity.lifecycle.addObserver(object: LifecycleEventObserver{
            override fun onStateChanged(
                source: LifecycleOwner,
                event: Lifecycle.Event
            ) {
                if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                    if (authJob != null) {
                        cancelJob?.cancel()
                        cancelJob = scope.launch {
                            delay(3000)
                            authJob?.cancel(CancellationException())
                            authJob = null
                        }
                    }
                }
            }
        })

        activity.addOnNewIntentListener { it ->
            it.data?.let { uri ->
                if (uri.host == getTelegramRedirect(activity.resources, isDebug)) {
                    cancelJob?.cancel()
                    cancelJob = null
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
        launchSignIn()
        authDataState.takeWhile { it.isLoading() }.collect()
        return authDataState.value
    }

    private suspend fun launchSignIn() {
        val safeActivity = weakActivity?.get() ?: return
        if (authDataState.value.isLoading()) {
            return
        }

        authDataState.value = Resource.Loading()
        authJob = scope.launch {
            TelegramLogin.startLogin(safeActivity)
            authDataState.takeWhile { it.isLoading() }.collect()
        }

        authJob?.invokeOnCompletion { e ->
            if (e != null) {
                authDataState.update {
                    if (e is CancellationException) {
                        Resource.Uninitialized()
                    } else {
                        Resource.Error(e)
                    }
                }
            }
        }
        authJob?.join()
        authJob = null
    }

    override fun launchSignOut() {
        authDataState.update { Resource.Uninitialized() }
    }
}
