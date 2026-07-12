package com.aglushkov.wordteacher.shared.workers

import com.aglushkov.wordteacher.shared.general.resource.Resource
import kotlinx.coroutines.flow.Flow

interface FillMisspellingDBController {
    val isLoaded: Boolean
    val loadingFlow: Flow<Resource<Unit>>

    fun reset()
}