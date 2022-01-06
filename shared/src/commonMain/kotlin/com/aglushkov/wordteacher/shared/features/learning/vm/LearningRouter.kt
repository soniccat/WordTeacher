package com.aglushkov.wordteacher.shared.features.learning.vm

import com.aglushkov.wordteacher.shared.general.SimpleRouter

interface LearningRouter: SimpleRouter {
    fun openSessionResult(results: List<SessionCardResult>)
}
