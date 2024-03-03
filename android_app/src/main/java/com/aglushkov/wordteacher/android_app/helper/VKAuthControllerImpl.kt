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
//    private var signInLauncher: ActivityResultLauncher<Collection<VKScope>>? = null
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
//        signInLauncher = VK.login(activity) { result ->
//            when (result) {
//                is VKAuthenticationResult.Failed -> {
//                    if (result.exception.isCanceled) {
//                        vkAuthDataState.value = Resource.Uninitialized()
//                    } else {
//                        vkAuthDataState.value = Resource.Error(result.exception, canTryAgain = true)
//                    }
//                }
//
//                is VKAuthenticationResult.Success -> {
//                    result.token.sav
//                }
//            }
//        }
//            activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
////            val intent = result.data
////            if (result.resultCode == Activity.RESULT_OK && intent != null) {
////                handleSignInResult(intent, safeClient)
////            } else {
////                vkAuthDataState.value = Resource.Uninitialized()
////            }
//                result.resultCode
//                result.resultCode
//            }

//        vkAuthDataState.value = Resource.Loading()
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
//        signInLauncher?.launch(listOf())

//        val client = client ?: return
//        val signInRequest = signInRequest ?: return
//        client.beginSignIn(signInRequest)
//            .addOnSuccessListener { result ->
//                try {
//                    signInLauncher?.launch(IntentSenderRequest.Builder(result.pendingIntent.intentSender).build())
//                } catch (e: IntentSender.SendIntentException) {
//                    Logger.e(
//                        "GoogleAuthRepository",
//                        "beginSignIn Couldn't start One Tap UI: ${e.localizedMessage}"
//                    )
//                    vkAuthDataState.value = Resource.Error(e, canTryAgain = true)
//                }
//            }
//            .addOnFailureListener {
//                Logger.e("GoogleAuthRepository", "beginSignIn failure")
//                vkAuthDataState.value = Resource.Error(it, canTryAgain = true)
//            }
//            .addOnCanceledListener {
//                Logger.v("GoogleAuthRepository", "beginSignIn cancelled")
//                vkAuthDataState.value = Resource.Uninitialized()
//            }
    }
//
//    private fun handleSignInResult(intent: Intent, client: SignInClient) {
//        try {
//            val creds = client.getSignInCredentialFromIntent(intent)
//            vkAuthDataState.value =
//                Resource.Loaded(VKAuthData(creds.displayName, creds.googleIdToken!!, false))
//        } catch (e: ApiException) {
//            vkAuthDataState.value = Resource.Error(e, canTryAgain = true)
//        }
//    }

    override fun launchSignOut() {
        vkAuthDataState.update { Resource.Uninitialized() }
    }
}
