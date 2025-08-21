package com.aglushkov.wordteacher.shared.features.learning.vm

import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.general.SimpleRouter

interface LearningRouter: SimpleRouter {
    fun openLearningSessionResult(results: List<SessionCardResult>)
    fun openDefinitions(state: DefinitionsVM.State)
}
