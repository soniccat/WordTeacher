package com.aglushkov.wordteacher.androidApp.features.article.views

import android.app.Application
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.databinding.FragmentArticleBinding
import com.aglushkov.wordteacher.androidApp.features.article.di.DaggerArticleComponent
import com.aglushkov.wordteacher.androidApp.features.definitions.views.DefinitionsAndroidVM
import com.aglushkov.wordteacher.androidApp.features.definitions.views.showPartsOfSpeechFilterChooser
import com.aglushkov.wordteacher.androidApp.general.AndroidVM
import com.aglushkov.wordteacher.androidApp.general.ViewItemBinder
import com.aglushkov.wordteacher.androidApp.general.extensions.getDrawableCompat
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveThemeInt
import com.aglushkov.wordteacher.androidApp.general.extensions.submit
import com.aglushkov.wordteacher.androidApp.general.views.bind
import com.aglushkov.wordteacher.di.AppComponentOwner
import com.aglushkov.wordteacher.di.ArticleBinder
import com.aglushkov.wordteacher.di.DefinitionsBinder
import com.aglushkov.wordteacher.shared.events.BackNavigationEvent
import com.aglushkov.wordteacher.shared.events.ErrorEvent
import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.ShowPartsOfSpeechFilterEvent
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import javax.inject.Inject


class ArticleAndroidVM(
    application: Application
): AndroidVM<ArticleVM>(application)

class ArticleFragment: DialogFragment() {
    private lateinit var androidVM: ArticleAndroidVM
    @Inject lateinit var articleVM: ArticleVM
    private lateinit var androidDefinitionsVM: DefinitionsAndroidVM
    @Inject lateinit var definitionsVM: DefinitionsVM
    private var binding: FragmentArticleBinding? = null

    @Inject lateinit var idGenerator: IdGenerator
    @Inject @ArticleBinder lateinit var articleBinder: ViewItemBinder
    @Inject @DefinitionsBinder lateinit var definitionsBinder: ViewItemBinder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidDefinitionsVM = ViewModelProvider(this).get(DefinitionsAndroidVM::class.java)
        androidVM = ViewModelProvider(this).get(ArticleAndroidVM::class.java)

        val id = requireArguments().getLong(STATE_ID)
        val vmState = savedInstanceState?.getParcelable(VM_STATE) ?: ArticleVM.State(id, DefinitionsVM.State())
        val deps = (requireContext().applicationContext as AppComponentOwner).appComponent
        val component = DaggerArticleComponent.builder()
            .setDeps(deps)
            .setDefinitionsDeps(deps)
            .setVMWrapper(androidVM)
            .setVMState(vmState)
            .setDefinitionsVMWrapper(androidDefinitionsVM)
            .setDefinitionsState(vmState.definitionsState)
            .setViewContext(requireContext())
            .build()
        if (!androidDefinitionsVM.isInitialized()) {
            component.injectDefinitionsViewModelWrapper(androidDefinitionsVM)
        }
        if (!androidVM.isInitialized()) {
            component.injectViewModelWrapper(androidVM)
        }
        component.injectArticleFragment(this)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            onBackPressed()
        }
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

        bindToolbar(binding)
        bindParagraphsList(binding)
        bindDefinitionsBottomSheet(binding)
        bindDefinitions(binding)

        viewLifecycleOwner.lifecycleScope.launch {
            launch {
                articleVM.article.collect {
                    when (it) {
                        is Resource.Loaded -> {
                            binding.toolbar.title = it.data.name
                        }
                    }
                }
            }
            launch {
                articleVM.paragraphs.collect {
                    when (it) {
                        // TODO: handle loading
                        is Resource.Loaded -> {
                            updateArticleAdapter(it)
                        }
                        else -> {
                        }
                    }
                }
            }
            launch {
                merge(articleVM.eventFlow, definitionsVM.eventFlow).collect {
                    handleEvent(it)
                }
            }
        }
    }

    private fun bindToolbar(binding: FragmentArticleBinding) {
        binding.toolbar.navigationIcon =
            requireContext().getDrawableCompat(R.drawable.ic_arrow_back_24)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

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
    }

    private fun onBackPressed() {
        if (isDefinitionsBottomSheetExpanded()) {
            hideDefinitionsBottomSheet()
        } else {
            articleVM.onBackPressed()
        }
    }

    private fun bindParagraphsList(binding: FragmentArticleBinding) {
        binding.paragraphsList.layoutManager = LinearLayoutManager(binding.root.context, RecyclerView.VERTICAL, false)
    }

    private fun bindDefinitionsBottomSheet(binding: FragmentArticleBinding) {
        val bottomSheetBehavior = bottomSheetBehavior(binding)
        hideDefinitionsBottomSheet()

        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
//                TODO("Not yet implemented")
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
//                TODO("Not yet implemented")
            }
        })
    }

    private fun bottomSheetBehavior(binding: FragmentArticleBinding) =
        BottomSheetBehavior.from(binding.definitionsBottomsheet)

    private fun bindDefinitions(binding: FragmentArticleBinding) {
        binding.definitionsList.layoutManager = LinearLayoutManager(binding.root.context, RecyclerView.VERTICAL, false)
        binding.defintionsLoader.setOnTryAgainListener {
            definitionsVM.onTryAgainClicked()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            definitionsVM.definitions.collect {
                val errorText = definitionsVM.getErrorText(it)?.toString(requireContext())
                it.bind(binding.defintionsLoader, errorText)

                when (it) {
                    is Resource.Loading,
                    is Resource.Loaded -> {
                        if (it is Resource.Loaded && it.data.isEmpty()) {
                            hideDefinitionsBottomSheet()
                        } else {
                            showDefinitionsBottomSheet()
                            updateDefinitionsAdapter(it)
                        }
                    }
                    is Resource.Error -> {
                        hideDefinitionsBottomSheet()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun updateArticleAdapter(it: Resource<List<BaseViewItem<*>>>) =
        binding!!.paragraphsList.submit(it, articleBinder)

    private fun updateDefinitionsAdapter(it: Resource<List<BaseViewItem<*>>>) =
        binding!!.definitionsList.submit(it, definitionsBinder)

    // Helpers

    private fun handleEvent(it: Event) {
        when (it) {
            is BackNavigationEvent -> {
                childFragmentManager.popBackStack()
            }
            is ErrorEvent -> {
                showError(it.text.toString(requireContext()))
            }
            is ShowPartsOfSpeechFilterEvent -> {
                showPartsOfSpeechFilterChooser(
                    requireContext(),
                    idGenerator,
                    definitionsVM,
                    it.partsOfSpeech,
                    it.selectedPartsOfSpeech
                )
            }
        }
    }

    private fun showError(text: String) {
        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
    }

    private fun showDefinitionsBottomSheet() {
        bottomSheetBehavior(binding!!).state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun hideDefinitionsBottomSheet() {
        bottomSheetBehavior(binding!!).state = BottomSheetBehavior.STATE_HIDDEN
        articleVM.onWordDefinitionHidden()
    }

    private fun isDefinitionsBottomSheetExpanded() =
        bottomSheetBehavior(binding!!).state == BottomSheetBehavior.STATE_EXPANDED

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