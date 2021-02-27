package com.aglushkov.wordteacher.androidApp.features.article.views

import android.app.Application
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.databinding.FragmentArticleBinding
import com.aglushkov.wordteacher.androidApp.features.article.di.DaggerArticleComponent
import com.aglushkov.wordteacher.androidApp.general.VMWrapper
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveThemeInt
import com.aglushkov.wordteacher.di.AppComponentOwner
import com.aglushkov.wordteacher.shared.events.CompletionEvent
import com.aglushkov.wordteacher.shared.events.ErrorEvent
import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.features.article.ArticleVM
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ArticleVMWrapper(
    application: Application
): VMWrapper<ArticleVM>(application)

class ArticleFragment: DialogFragment() {
    private lateinit var androidVM: ArticleVMWrapper
    private lateinit var articleVM: ArticleVM
    private var binding: FragmentArticleBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidVM = ViewModelProvider(this)
                .get(ArticleVMWrapper::class.java)

        val id = requireArguments().getLong(STATE_ID)
        val vmState = savedInstanceState?.getParcelable(VM_STATE) ?: ArticleVM.State(id)
        val deps = (requireContext().applicationContext as AppComponentOwner).appComponent
        val component = DaggerArticleComponent.builder()
            .setDeps(deps)
            .setVMState(vmState)
            .setVMWrapper(androidVM)
            .build()
        if (!androidVM.isInitialized()) {
            component.injectViewModelWrapper(androidVM)
        }
        component.injectArticleFragment(this)

        articleVM = androidVM.vm
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
        binding = FragmentArticleBinding.inflate(inflater, null, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(VM_STATE, articleVM.state)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
    }

    private fun bindView() {
        val binding = binding!!

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.close -> {
                    //TODO:
                    //articleVM.onCancelPressed()
                    true
                }
                else -> false
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            articleVM.eventFlow.collect {
                handleEvent(it)
            }
        }
    }

    private fun handleEvent(it: Event) {
        when (it) {
            is CompletionEvent -> {
                dismiss()
            }
            is ErrorEvent -> {
                showError(it.text.toString(requireContext()))
            }
        }
    }

    private fun showError(text: String) {
        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }


    companion object {
        fun createArguments(id: Long) = Bundle().apply {
            putLong(STATE_ID, id)
        }
    }
}

private val VM_STATE = "vm_state"
private val STATE_ID = "state_id"