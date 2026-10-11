package com.aglushkov.wordteacher.shared.events

import com.aglushkov.wordteacher.shared.general.resource.StringResourceWrapper


// TODO: replace with SnackbarEventHolder
data class ErrorEvent(val text: StringResourceWrapper) : Event