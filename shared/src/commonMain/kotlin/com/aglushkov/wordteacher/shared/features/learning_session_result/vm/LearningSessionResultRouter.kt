package com.aglushkov.wordteacher.shared.features.learning_session_result.vm

import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.general.SimpleRouter

interface LearningSessionResultRouter: SimpleRouter {
    fun openDefinitions(state: DefinitionsVM.State)
}
