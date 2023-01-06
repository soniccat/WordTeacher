package com.aglushkov.wordteacher.androidApp.helper

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.extensions.takeUntilLoadedOrErrorForVersion
import com.aglushkov.wordteacher.shared.general.resource.*
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
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

class GoogleAuthRepository(
    private val serverClientId: String
) {
    private var client: GoogleSignInClient? = null
    private var signInLauncher: ActivityResultLauncher<Intent>? = null
    private var googleSignInAccountState: MutableStateFlow<Resource<GoogleSignInAccount>> = MutableStateFlow(Resource.Uninitialized())
    var googleSignInAccountFlow: StateFlow<Resource<GoogleSignInAccount>> = googleSignInAccountState
//    private var activityRef: WeakReference<Activity> = WeakReference(null)

    fun bind(activity: ComponentActivity) {
        client = GoogleSignIn.getClient(activity, createGoogleSignInOptions())
        signInLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
                handleSignInResult(task)
            } else {
                googleSignInAccountState.value = Resource.Uninitialized()
            }
        }
    }

//    suspend fun loadSignInAccount(): Resource<GoogleSignInAccount> {
//        var googleSigninAccount: Resource<> = googleSignInAccountFlow.value.asLoaded()
//        if (googleSigninAccount == null) {
//            signIn()
//            googleSigninAccount = googleSignInAccountFlow.takeUntilLoadedOrErrorForVersion().last()
//        }
//        return googleSigninAccount
//    }

    fun signIn() {
        if (googleSignInAccountState.value.isLoading()) {
            return
        }
        googleSignInAccountState.value = Resource.Loading()

        val client = client ?: return
        signInLauncher?.launch(client.signInIntent)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            googleSignInAccountState.value = Resource.Loaded(account)
        } catch (e: ApiException) {
            googleSignInAccountState.value = Resource.Error(e, canTryAgain = true)
        }
    }

    private fun createGoogleSignInOptions() =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail() // TODO: check if we really need any of them
            .requestProfile()
            //.requestScopes(new Scope("https://www.googleapis.com/auth/contacts.readonly"))
            //.requestScopes(new Scope(Scopes.DRIVE_APPFOLDER))
            //.requestServerAuthCode(getResources().getString(R.string.server_client_id))
            .requestIdToken(serverClientId)
            .build()
}
