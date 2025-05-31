package com.aglushkov.wordteacher.android_app.features.learning_session_result.views

import com.aglushkov.wordteacher.shared.features.learning_session_result.vm.LearningSessionResultRouter
import com.aglushkov.wordteacher.shared.features.learning_session_result.vm.LearningSessionResultVM
import com.aglushkov.wordteacher.shared.features.learning_session_result.vm.LearningSessionTermResultViewItem
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LearningSessionResultVMPreview: LearningSessionResultVM {
    override var router: LearningSessionResultRouter?
        get() = null
        set(value) {}

    override val viewItems: StateFlow<Resource<List<BaseViewItem<*>>>>
        get() = MutableStateFlow(Resource.Uninitialized())

    override fun onTermClicked(item: LearningSessionTermResultViewItem) {
    }

    override fun onTryAgainClicked() {
    }

    override fun onCloseClicked() {
    }

    override fun getErrorText(res: Resource<List<BaseViewItem<*>>>): StringDesc? {
        return null
    }

    override fun onCleared() {
    }
}