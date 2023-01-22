package com.aglushkov.wordteacher.android_app.general

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.aglushkov.wordteacher.shared.general.ViewModel
import javax.inject.Inject

open class AndroidVM<T: Any>(
    application: Application
): AndroidViewModel(application) {

    @Inject lateinit var vm: T

    fun isInitialized() = ::vm.isInitialized

    override fun onCleared() {
        super.onCleared()
        (vm as? ViewModel)?.onCleared()
    }
}