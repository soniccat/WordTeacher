package com.aglushkov.wordteacher.androidApp.general

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import dev.icerock.moko.mvvm.viewmodel.ViewModel
import javax.inject.Inject

open class VMWrapper<T: ViewModel>(
    application: Application
): AndroidViewModel(application) {

    @Inject lateinit var vm: T

    fun isInitialized() = ::vm.isInitialized

    // TODO: create a base class for a MokoVM wrapper
    override fun onCleared() {
        super.onCleared()
        vm.onCleared()
    }
}