package com.aglushkov.wordteacher.shared.features.webauth

import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.webauth.vm.WebAuthVMImpl
import com.aglushkov.wordteacher.shared.general.GoogleAuthController
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.arkivanov.decompose.ComponentContext

class WebAuthDecomposeComponent (
    componentContext: ComponentContext,
    configuration: MainDecomposeComponent.ChildConfiguration.WebAuthConfiguration,
    timeSource: TimeSource
) : WebAuthVMImpl(
    configuration.networkType,
    timeSource
), ComponentContext by componentContext {
}
