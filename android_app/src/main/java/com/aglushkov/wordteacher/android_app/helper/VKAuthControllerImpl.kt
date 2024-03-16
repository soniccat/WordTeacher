package com.aglushkov.wordteacher.android_app.helper

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import androidx.activity.ComponentActivity
import com.aglushkov.wordteacher.shared.general.VKAuthController
import com.aglushkov.wordteacher.shared.general.VKAuthData
import com.aglushkov.wordteacher.shared.general.auth.NetworkAuthController
import com.aglushkov.wordteacher.shared.general.auth.NetworkAuthData
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoadedOrError
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.vk.id.AccessToken
import com.vk.id.VKID
import com.vk.id.VKIDAuthFail
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

    private val vkAuthCallback = object : VKID.AuthCallback {
        override fun onSuccess(accessToken: AccessToken) {
            vkAuthDataState.update {
                Resource.Loaded(
                    VKAuthData(
                        token = accessToken.token,
                        userID = accessToken.userID,
                        expireTime = accessToken.expireTime,
                        firstName = accessToken.userData.firstName,
                        lastName = accessToken.userData.lastName,
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
        vkid = VKID(activity)
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
        vkid!!.authorize(authCallback = vkAuthCallback)
    }

    override fun launchSignOut() {
        vkAuthDataState.update { Resource.Uninitialized() }
    }
}
