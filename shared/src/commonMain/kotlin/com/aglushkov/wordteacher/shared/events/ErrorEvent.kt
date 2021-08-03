package com.aglushkov.wordteacher.shared.events

import com.aglushkov.resources.desc.StringDesc

data class ErrorEvent(val text: StringDesc) : Event