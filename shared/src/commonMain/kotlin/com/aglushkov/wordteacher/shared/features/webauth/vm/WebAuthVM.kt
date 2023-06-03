package com.aglushkov.wordteacher.shared.features.webauth.vm

import com.aglushkov.wordteacher.shared.features.AuthOpener
import com.aglushkov.wordteacher.shared.general.GoogleAuthController
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.service.SpaceAuthService
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize

interface WebAuthVM {
    var router: WebAuthRouter?

    fun onCompleted(result: AuthOpener.AuthResult)
    fun onError(throwable: Throwable)

    @Parcelize
    class State: Parcelable {
    }
}

open class WebAuthVMImpl(
    private val networkType: SpaceAuthService.NetworkType,
    private val timeSource: TimeSource,
): ViewModel(), WebAuthVM {
    override var router: WebAuthRouter? = null

    override fun onCompleted(result: AuthOpener.AuthResult) {
        router?.onCompleted(result)
    }

    override fun onError(throwable: Throwable) {
        router?.onError(throwable)
    }
}
