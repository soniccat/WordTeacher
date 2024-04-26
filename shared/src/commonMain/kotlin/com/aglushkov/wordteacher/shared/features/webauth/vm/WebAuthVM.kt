package com.aglushkov.wordteacher.shared.features.webauth.vm

import com.aglushkov.wordteacher.shared.features.AuthOpener
import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.general.auth.GoogleAuthData
import com.aglushkov.wordteacher.shared.general.oauth2.OAuth2Service
import com.aglushkov.wordteacher.shared.service.SpaceAuthService
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface WebAuthVM: Clearable {
    var router: WebAuthRouter?
    val initialUrl: Url
    val state: StateFlow<InMemoryState>

    fun onCompleted(result: AuthOpener.AuthResult)
    fun onError(throwable: Throwable)
    fun onUrlChanged(url: String)

    @Parcelize
    class State: Parcelable {
    }

    data class InMemoryState(
        val isCompleted: Boolean
    )
}

open class WebAuthVMImpl(
    private val networkType: SpaceAuthService.NetworkType,
    private val timeSource: TimeSource,
    private val googleOAuth2Service: OAuth2Service,
): ViewModel(), WebAuthVM {
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override var router: WebAuthRouter? = null
    private val authContext = googleOAuth2Service.buildAuthContext()

    override val initialUrl: Url = authContext.url
    override val state: MutableStateFlow<WebAuthVM.InMemoryState> = MutableStateFlow(WebAuthVM.InMemoryState(isCompleted = false))

    override fun onUrlChanged(url: String) {
        when(val result = googleOAuth2Service.parseAuthResponseUrl(url, authContext.state)) {
            is OAuth2Service.AuthResult.Success -> {
                mainScope.launch {
                    try {
                        val token = googleOAuth2Service.accessToken(result.code, authContext)
                        Logger.v("token " + token.accessToken)
                        onCompleted(
                            AuthOpener.AuthResult.GoogleResult(
                                data = GoogleAuthData(
                                    name = "Google Account",
                                    tokenId = token.idToken.orEmpty(),
                                    isSilent = false,
                                )
                            )
                        )
                    } catch (e: Exception) {
                        // TODO: handle error
                        val txt = "accessToken error: " + e.message
                        Logger.e(txt)
                        onError(RuntimeException(txt))
                    }
                }
            }
            is OAuth2Service.AuthResult.Error -> {
                // TODO: handle error
                val txt = "error result: " + result.error
                Logger.e(txt)
                onError(RuntimeException(txt))
            }
            is OAuth2Service.AuthResult.WrongUrl -> {
                // skip
            }
            else -> {
                // TODO: handle error
                val txt = "sth went wrong in parseAuthResponseUrl: $result"
                Logger.e(txt)
                onError(RuntimeException(txt))
            }
        }
    }

    override fun onCompleted(result: AuthOpener.AuthResult) {
        state.update { it.copy(isCompleted = true) }
        router?.onCompleted(result)
    }

    override fun onError(throwable: Throwable) {
        router?.onError(throwable)
    }
}
