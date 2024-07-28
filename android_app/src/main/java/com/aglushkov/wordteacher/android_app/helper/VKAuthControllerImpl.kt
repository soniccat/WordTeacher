package com.aglushkov.wordteacher.android_app.helper

import androidx.activity.ComponentActivity
import com.aglushkov.wordteacher.shared.general.auth.VKAuthController
import com.aglushkov.wordteacher.shared.general.auth.VKAuthData
import com.aglushkov.wordteacher.shared.general.auth.NetworkAuthData
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoadedOrError
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.vk.id.AccessToken
import com.vk.id.VKID
import com.vk.id.VKIDAuthFail
import com.vk.id.auth.VKIDAuthCallback
import com.vk.id.auth.VKIDAuthParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import java.lang.RuntimeException

class VKAuthControllerImpl : VKAuthController {
    private var vkid: VKID? = null
    private var vkAuthDataState: MutableStateFlow<Resource<NetworkAuthData>> =
        MutableStateFlow(Resource.Uninitialized())
    override var authDataFlow: StateFlow<Resource<NetworkAuthData>> = vkAuthDataState

    private val vkAuthCallback = object : VKIDAuthCallback {
        override fun onAuth(accessToken: AccessToken) {
            vkAuthDataState.update {
                Resource.Loaded(
                    VKAuthData(
                        token = accessToken.token,
                        userID = accessToken.userID,
                        expireTime = accessToken.expireTime,
                    )
                )
            }
        }

        override fun onFail(fail: VKIDAuthFail) {
            when (fail) {
                is VKIDAuthFail.Canceled -> {
                    vkAuthDataState.update { Resource.Uninitialized() }
                }
                else -> {
                    vkAuthDataState.update { Resource.Error(RuntimeException(fail.description)) }
                }
            }
        }
    }

    fun bind(activity: ComponentActivity) {
        vkid = VKID.instance
    }

    override suspend fun signIn(): Resource<NetworkAuthData> {
        launchSignIn()
        vkAuthDataState.takeWhile { !it.isLoadedOrError() }.collect()
        return vkAuthDataState.value
    }

    private suspend fun launchSignIn() {
        if (vkAuthDataState.value.isLoading()) {
            return
        }
        vkAuthDataState.value = Resource.Loading()
        vkid!!.authorize(callback = vkAuthCallback, params = VKIDAuthParams {})
    }

    override fun launchSignOut() {
        vkAuthDataState.update { Resource.Uninitialized() }
    }
}
