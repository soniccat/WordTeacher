package com.aglushkov.wordteacher.shared.features.webauth

import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.webauth.vm.WebAuthVMImpl
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.oauth2.OAuth2Service
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy

class WebAuthDecomposeComponent (
    componentContext: ComponentContext,
    configuration: MainDecomposeComponent.ChildConfiguration.WebAuthConfiguration,
    timeSource: TimeSource,
    googleOAuth2Service: OAuth2Service,
) : WebAuthVMImpl(
    configuration.networkType,
    timeSource,
    googleOAuth2Service,
), ComponentContext by componentContext {
    init {
        lifecycle.doOnDestroy {
            onCleared()
        }
    }
}
