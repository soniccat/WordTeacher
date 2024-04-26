package com.aglushkov.wordteacher.android_app.helper

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.aglushkov.wordteacher.shared.general.auth.GoogleAuthData
import com.aglushkov.wordteacher.shared.general.auth.GoogleAuthController
import com.aglushkov.wordteacher.shared.general.auth.NetworkAuthData
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoadedOrError
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.aglushkov.wordteacher.shared.general.v
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile

class GoogleAuthControllerImpl(
    private val serverClientId: String
): GoogleAuthController {
    private var oldClient: GoogleSignInClient? = null
    private var client: SignInClient? = null
    private var signInRequest: BeginSignInRequest? = null
    private var signInLauncher: ActivityResultLauncher<IntentSenderRequest>? = null
    private var googleAuthDataState: MutableStateFlow<Resource<NetworkAuthData>> = MutableStateFlow(Resource.Uninitialized())
    override var authDataFlow: StateFlow<Resource<NetworkAuthData>> = googleAuthDataState

    fun bind(activity: ComponentActivity) {
        val safeClient = Identity.getSignInClient(activity)
        client = safeClient
        signInLauncher = activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            val intent = result.data
            if (result.resultCode == Activity.RESULT_OK && intent != null) {
                handleSignInResult(intent, safeClient)
            } else {
                googleAuthDataState.value = Resource.Uninitialized()
            }
        }

        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(serverClientId)
                    // Only show accounts previously used to sign in.
                    .setFilterByAuthorizedAccounts(true)
                    .build()
            )
            // Automatically sign in when exactly one credential is retrieved.
//            .setAutoSelectEnabled(true)
            .build()

        val safeGoogleSignInClient = GoogleSignIn.getClient(activity, createGoogleSignInOptions())
        oldClient = safeGoogleSignInClient

        googleAuthDataState.value = Resource.Loading()
        safeGoogleSignInClient.silentSignIn()
            .addOnSuccessListener { acc ->
                acc.idToken?.let { idToken ->
                    Logger.v("GoogleAuthRepository", "silentSignIn success ${acc.idToken}")
                    googleAuthDataState.value = Resource.Loaded(GoogleAuthData(acc.displayName, idToken, true))
                } ?: run {
                    Logger.e("GoogleAuthRepository", "idToken is null")
                    googleAuthDataState.value = Resource.Error(RuntimeException("Google idToken is null"), canTryAgain = true)
                }
            }
            .addOnFailureListener {
                Logger.e("GoogleAuthRepository", "silentSignIn failure")
                googleAuthDataState.value = Resource.Error(it, canTryAgain = true)
            }
            .addOnCanceledListener {
                Logger.v("GoogleAuthRepository", "silentSignIn cancelled")
                googleAuthDataState.value = Resource.Uninitialized()
            }
    }

    override suspend fun signIn(): Resource<NetworkAuthData> {
        launchSignIn()
        googleAuthDataState.takeWhile { !it.isLoadedOrError() }.collect()
        return googleAuthDataState.value
    }

    private fun launchSignIn() {
        if (googleAuthDataState.value.isLoading()) {
            return
        }
        googleAuthDataState.value = Resource.Loading()

        val client = client ?: return
        val signInRequest = signInRequest ?: return
        client.beginSignIn(signInRequest)
            .addOnSuccessListener { result ->
                try {
                    signInLauncher?.launch(IntentSenderRequest.Builder(result.pendingIntent.intentSender).build())
                } catch (e: IntentSender.SendIntentException) {
                    Logger.e(
                        "GoogleAuthRepository",
                        "beginSignIn Couldn't start One Tap UI: ${e.localizedMessage}"
                    )
                    googleAuthDataState.value = Resource.Error(e, canTryAgain = true)
                }
            }
            .addOnFailureListener {
                Logger.e("GoogleAuthRepository", "beginSignIn failure")
                googleAuthDataState.value = Resource.Error(it, canTryAgain = true)
            }
            .addOnCanceledListener {
                Logger.v("GoogleAuthRepository", "beginSignIn cancelled")
                googleAuthDataState.value = Resource.Uninitialized()
            }
    }

    private fun handleSignInResult(intent: Intent, client: SignInClient) {
        try {
            val creds = client.getSignInCredentialFromIntent(intent)
            googleAuthDataState.value = Resource.Loaded(GoogleAuthData(creds.displayName, creds.googleIdToken!!, false))
        } catch (e: ApiException) {
            googleAuthDataState.value = Resource.Error(e, canTryAgain = true)
        }
    }

    override fun launchSignOut() {
        val prevValue = googleAuthDataState.value
        val safeClient = oldClient ?: return
        safeClient.signOut()
            .addOnSuccessListener {
                googleAuthDataState.value = Resource.Uninitialized()
            }
            .addOnFailureListener {
                googleAuthDataState.value = prevValue
            }
            .addOnCanceledListener {
                googleAuthDataState.value = prevValue
            }
    }

    private fun createGoogleSignInOptions() =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestIdToken(serverClientId)
            .build()
}
