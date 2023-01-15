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
import kotlinx.coroutines.flow.*

//class GoogleAuthResultContract(
//    private val client: GoogleSignInClient
//) : ActivityResultContract<Unit, Task<GoogleSignInAccount>?>() {
//    override fun createIntent(context: Context, input: Unit) =
//        client.signInIntent
//
//    override fun parseResult(resultCode: Int, result: Intent?) : Task<GoogleSignInAccount>? {
//        if (resultCode != Activity.RESULT_OK) {
//            return null
//        }
//
//        // The Task returned from this call is always completed, no need to attach
//        // a listener.
//        return GoogleSignIn.getSignedInAccountFromIntent(result)
//    }
//}

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
        safeGoogleSignInClient.silentSignIn()
            .addOnSuccessListener { acc ->
                Log.e("GoogleAuthRepository", "silentSignIn success ${acc.idToken}")
                googleSignInCredentialState.value = Resource.Loaded(GoogleAuthData(acc.displayName, acc.idToken, true))

            }
            .addOnFailureListener {
                Log.e("GoogleAuthRepository", "silentSignIn failure")

            }
            .addOnCanceledListener {
                Log.e("GoogleAuthRepository", "silentSignIn cancelled")

            }
    }

    fun signIn() {
        if (googleSignInCredentialState.value.isLoading()) {
            return
        }
        googleSignInCredentialState.value = Resource.Loading()

        val client = client ?: return
        val signInRequest = signInRequest ?: return
//        signInLauncher?.launch(client.signInIntent)
        client.beginSignIn(signInRequest)
            .addOnSuccessListener { result ->
                try {
                    signInLauncher?.launch(
                        IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                    )
//                    signInLauncher.launch(result.pendingIntent.intentSender)
//                }
//                    startIntentSenderForResult(
//                        result.pendingIntent.intentSender, REQ_ONE_TAP,
//                        null, 0, 0, 0, null)
                } catch (e: IntentSender.SendIntentException) {
                    Log.e("GoogleAuthRepository", "Couldn't start One Tap UI: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener {
                Log.e("GoogleAuthRepository", "failure")
            }
            .addOnCanceledListener {
                Log.e("GoogleAuthRepository", "cancelled")
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

    private fun createGoogleSignInOptions() =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail() // TODO: check if we really need any of them
            .requestProfile()
            .requestIdToken(serverClientId)
            .build()
}

private const val REQ_ONE_TAP = 2