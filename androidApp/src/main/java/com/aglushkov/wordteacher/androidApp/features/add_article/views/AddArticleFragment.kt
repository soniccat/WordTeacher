package com.aglushkov.wordteacher.androidApp.features.add_article.views

import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import com.aglushkov.wordteacher.androidApp.databinding.FragmentArticlesBinding
import com.aglushkov.wordteacher.androidApp.features.add_article.di.DaggerAddArticleComponent
import com.aglushkov.wordteacher.di.AppComponentOwner
import com.aglushkov.wordteacher.shared.features.add_article.AddArticleVM

import javax.inject.Inject

class AddArticleVMWrapper(
    application: Application
): AndroidViewModel(application) {

    @Inject lateinit var vm: AddArticleVM

    fun isInitialized() = ::vm.isInitialized
}

class AddArticleFragment: Fragment() {
    private lateinit var androidVM: AddArticleVMWrapper
    private lateinit var addArticleVM: AddArticleVM
    private var binding: FragmentArticlesBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidVM = ViewModelProvider(this)
                .get(AddArticleVMWrapper::class.java)

        val vmState = savedInstanceState?.getParcelable(VM_STATE) ?: AddArticleVM.State()
        val deps = (requireContext().applicationContext as AppComponentOwner).appComponent
        val component = DaggerAddArticleComponent.builder()
            .setDeps(deps)
            .setVMState(vmState)
            .setVMWrapper(androidVM)
            .build()
        if (!androidVM.isInitialized()) {
            component.injectViewModelWrapper(androidVM)
        }
        component.injectAddArticleFragment(this)

        addArticleVM = androidVM.vm
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(VM_STATE, addArticleVM.state)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentArticlesBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindView()
    }

    private fun bindView() {
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}

private val VM_STATE = "vm_state"