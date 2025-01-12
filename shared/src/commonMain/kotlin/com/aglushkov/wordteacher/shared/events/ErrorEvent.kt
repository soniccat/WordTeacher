package com.aglushkov.wordteacher.shared.events

import dev.icerock.moko.resources.desc.StringDesc

// TODO: replace with SnackbarEventHolder
data class ErrorEvent(val text: StringDesc) : Event