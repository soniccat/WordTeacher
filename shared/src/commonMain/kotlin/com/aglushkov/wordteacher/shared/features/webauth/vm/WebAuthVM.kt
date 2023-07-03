package com.aglushkov.wordteacher.shared.features.webauth.vm

import com.aglushkov.wordteacher.shared.features.AuthOpener
import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.general.oauth2.OAuth2Service
import com.aglushkov.wordteacher.shared.service.SpaceAuthService
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

interface WebAuthVM {
    var router: WebAuthRouter?
    val initialUrl: Url

    fun onCompleted(result: AuthOpener.AuthResult)
    fun onError(throwable: Throwable)
    fun onUrlChanged(url: String)

    @Parcelize
    class State: Parcelable {
    }
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

    override fun onUrlChanged(url: String) {
        when(val result = googleOAuth2Service.parseAuthResponseUrl(url)) {
            is OAuth2Service.AuthResult.Success -> {
                mainScope.launch {
                    try {
                        val token = googleOAuth2Service.accessToken(result.code, authContext)
                        Logger.v("token " + token.accessToken)
                    } catch (e: Exception) {
                        // TODO: handle error
                        Logger.e("accessToken error: " + e.message)
                    }
                }
            }
            is OAuth2Service.AuthResult.Error -> {
                // TODO: handle error
                Logger.e("error result: " + result.error)
            }
            is OAuth2Service.AuthResult.WrongUrl -> {
                // skip
            }
            else -> {
                // TODO: handle error
                Logger.e("sth went wrong in parseAuthResponseUrl: " + result)
            }
        }
    }

    override fun onCompleted(result: AuthOpener.AuthResult) {
        router?.onCompleted(result)
    }

    override fun onError(throwable: Throwable) {
        router?.onError(throwable)
    }
}
