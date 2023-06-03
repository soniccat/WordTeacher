package com.aglushkov.wordteacher.shared.features.webauth.vm

import com.aglushkov.wordteacher.shared.features.AuthOpener

interface WebAuthRouter {
    fun onCompleted(result: AuthOpener.AuthResult)
    fun onError(throwable: Throwable)
}
