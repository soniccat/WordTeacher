package com.aglushkov.wordteacher.shared.features.settings.vm

import dev.icerock.moko.resources.desc.StringDesc

interface SettingsRouter {
    fun openDictConfigs()
    fun onError(text: StringDesc)
}