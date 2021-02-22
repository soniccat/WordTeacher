package com.aglushkov.wordteacher.androidApp.features.add_article.views

import android.app.Application
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.databinding.FragmentAddArticleBinding
import com.aglushkov.wordteacher.androidApp.features.add_article.di.DaggerAddArticleComponent
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveThemeInt
import com.aglushkov.wordteacher.di.AppComponentOwner
import com.aglushkov.wordteacher.shared.events.CompletionEvent
import com.aglushkov.wordteacher.shared.features.add_article.AddArticleVM
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject


class AddArticleVMWrapper(
    application: Application
): AndroidViewModel(application) {

    @Inject lateinit var vm: AddArticleVM

    fun isInitialized() = ::vm.isInitialized
}

class AddArticleFragment: DialogFragment() {
    private lateinit var androidVM: AddArticleVMWrapper
    private lateinit var addArticleVM: AddArticleVM
    private var binding: FragmentAddArticleBinding? = null

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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), R.style.DialogStyle)

        // Apply layout_width and layout_height from the style as otherwise they are simply ignored
        dialog.window?.attributes?.let {
            it.width = dialog.context.resolveThemeInt(android.R.attr.layout_width)
            it.height = dialog.context.resolveThemeInt(android.R.attr.layout_height)
            dialog.window?.attributes = it
        }

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val inflater = LayoutInflater.from(context)
        binding = FragmentAddArticleBinding.inflate(inflater, null, false)

        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(VM_STATE, addArticleVM.state)
    }

    private fun bindView() {
        val binding = binding!!

        binding.titleField.doOnTextChanged { text, start, count, after ->
            addArticleVM.onTitleChanged(text?.toString().orEmpty())
        }

        binding.textField.doOnTextChanged { text, start, count, after ->
            addArticleVM.onTextChanged(text?.toString().orEmpty())
        }

        binding.doneButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                addArticleVM.onCompletePressed()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            addArticleVM.eventFlow.collect {
                when (it) {
                    is CompletionEvent -> {
                        dismiss()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            addArticleVM.completeButtonEnabled.collect { isEnabled ->
                binding.doneButton.isEnabled = isEnabled
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}

private val VM_STATE = "vm_state"