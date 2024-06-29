package com.aglushkov.wordteacher.shared.features

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.general.Clearable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.lifecycle.doOnResume

interface BaseDecomposeComponent: ComponentContext, Clearable {
    val componentName: String

    fun baseInit(analytics: Analytics) {
        doOnResume {
            analytics.sendScreen(componentName)
        }
        doOnDestroy { onCleared() }
    }
}