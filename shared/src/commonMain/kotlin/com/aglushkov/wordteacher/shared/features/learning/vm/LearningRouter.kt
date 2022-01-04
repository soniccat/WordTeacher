package com.aglushkov.wordteacher.shared.features.learning.vm

interface LearningRouter {
    fun openSessionResult(results: List<SessionCardResult>)
    fun closeLearning()
}
