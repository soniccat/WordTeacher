package com.aglushkov.wordteacher.shared.events

import dev.icerock.moko.resources.desc.StringDesc

data class ErrorEvent(val text: StringDesc) : Event