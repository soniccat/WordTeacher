package com.aglushkov.wordteacher.androidApp.helper

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.aglushkov.wordteacher.shared.general.resource.*
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.internal.BaseGmsClient.SignOutCallbacks
import kotlinx.coroutines.flow.*

data class GoogleAuthData(val name: String?, val tokenId: String?, val isSilent: Boolean)

class GoogleAuthRepository(
    private val serverClientId: String
) {
    private var oldClient: GoogleSignInClient? = null
    private var client: SignInClient? = null
    private var signInRequest: BeginSignInRequest? = null
    private var signInLauncher: ActivityResultLauncher<IntentSenderRequest>? = null
    private var googleSignInCredentialState: MutableStateFlow<Resource<GoogleAuthData>> = MutableStateFlow(Resource.Uninitialized())
    var googleSignInCredentialFlow: StateFlow<Resource<GoogleAuthData>> = googleSignInCredentialState

    fun bind(activity: ComponentActivity) {
        val safeClient = Identity.getSignInClient(activity)
        client = safeClient
        signInLauncher = activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            val intent = result.data
            if (result.resultCode == Activity.RESULT_OK && intent != null) {
                handleSignInResult(intent, safeClient)
            } else {
                googleSignInCredentialState.value = Resource.Uninitialized()
            }
        }

        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(serverClientId)
                    // Only show accounts previously used to sign in.
                    .setFilterByAuthorizedAccounts(true)
                    .build())
            // Automatically sign in when exactly one credential is retrieved.
//            .setAutoSelectEnabled(true)
            .build()

        val safeGoogleSignInClient = GoogleSignIn.getClient(activity, createGoogleSignInOptions())
        oldClient = safeGoogleSignInClient

        googleSignInCredentialState.value = Resource.Loading()
        safeGoogleSignInClient.silentSignIn()
            .addOnSuccessListener { acc ->
                Log.e("GoogleAuthRepository", "silentSignIn success ${acc.idToken}")
                googleSignInCredentialState.value = Resource.Loaded(GoogleAuthData(acc.displayName, acc.idToken, true))
            }
            .addOnFailureListener {
                Log.e("GoogleAuthRepository", "silentSignIn failure")
                googleSignInCredentialState.value = Resource.Error(it, canTryAgain = true)
            }
            .addOnCanceledListener {
                Log.e("GoogleAuthRepository", "silentSignIn cancelled")
                googleSignInCredentialState.value = Resource.Uninitialized()
            }
    }

    fun signIn() {
        if (googleSignInCredentialState.value.isLoading()) {
            return
        }
        googleSignInCredentialState.value = Resource.Loading()

        val client = client ?: return
        val signInRequest = signInRequest ?: return
        client.beginSignIn(signInRequest)
            .addOnSuccessListener { result ->
                try {
                    signInLauncher?.launch(
                        IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                    )
                } catch (e: IntentSender.SendIntentException) {
                    Log.e("GoogleAuthRepository", "beginSignIn Couldn't start One Tap UI: ${e.localizedMessage}")
                    googleSignInCredentialState.value = Resource.Error(e, canTryAgain = true)
                }
            }
            .addOnFailureListener {
                Log.e("GoogleAuthRepository", "beginSignIn failure")
                googleSignInCredentialState.value = Resource.Error(it, canTryAgain = true)
            }
            .addOnCanceledListener {
                Log.e("GoogleAuthRepository", "beginSignIn cancelled")
                googleSignInCredentialState.value = Resource.Uninitialized()
            }
    }

    private fun handleSignInResult(intent: Intent, client: SignInClient) {
        try {
            val creds = client.getSignInCredentialFromIntent(intent)
            googleSignInCredentialState.value = Resource.Loaded(GoogleAuthData(creds.displayName, creds.googleIdToken, false))
        } catch (e: ApiException) {
            googleSignInCredentialState.value = Resource.Error(e, canTryAgain = true)
        }
    }

    fun signOut() {
        val prevValue = googleSignInCredentialState.value
        val safeClient = oldClient ?: return
        safeClient.signOut()
            .addOnSuccessListener {
                googleSignInCredentialState.value = Resource.Uninitialized()
            }
            .addOnFailureListener {
                googleSignInCredentialState.value = prevValue
            }
            .addOnCanceledListener {
                googleSignInCredentialState.value = prevValue
            }
    }

    private fun createGoogleSignInOptions() =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestIdToken(serverClientId)
            .build()
}
