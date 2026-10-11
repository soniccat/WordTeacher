package com.aglushkov.wordteacher.shared.features.settings.vm

import com.aglushkov.wordteacher.shared.general.resource.StringResourceWrapper
import org.jetbrains.compose.resources.StringResource

interface SettingsRouter {
    fun openDictConfigs()
    fun onError(text: StringResourceWrapper)
}