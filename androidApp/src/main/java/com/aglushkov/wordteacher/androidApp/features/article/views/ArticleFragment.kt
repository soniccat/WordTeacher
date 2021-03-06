package com.aglushkov.wordteacher.androidApp.features.article.views

import android.app.Application
import android.app.Dialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.databinding.FragmentArticleBinding
import com.aglushkov.wordteacher.androidApp.features.article.di.DaggerArticleComponent
import com.aglushkov.wordteacher.androidApp.features.definitions.views.DefinitionsVMWrapper
import com.aglushkov.wordteacher.androidApp.general.SimpleAdapter
import com.aglushkov.wordteacher.androidApp.general.VMWrapper
import com.aglushkov.wordteacher.androidApp.general.ViewItemBinder
import com.aglushkov.wordteacher.androidApp.general.extensions.getDrawableCompat
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveThemeInt
import com.aglushkov.wordteacher.androidApp.general.views.bind
import com.aglushkov.wordteacher.di.AppComponentOwner
import com.aglushkov.wordteacher.shared.events.BackNavigationEvent
import com.aglushkov.wordteacher.shared.events.ErrorEvent
import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.features.article.vm.ShowDefinitionEvent
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject


class ArticleVMWrapper(
    application: Application
): VMWrapper<ArticleVM>(application)

class ArticleFragment: DialogFragment() {
    private lateinit var androidVM: ArticleVMWrapper
    @Inject lateinit var articleVM: ArticleVM
    private lateinit var androidDefinitionsVM: DefinitionsVMWrapper
    @Inject lateinit var definitionsVM: DefinitionsVM
    private var binding: FragmentArticleBinding? = null

    @Inject lateinit var binder: ViewItemBinder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidDefinitionsVM = ViewModelProvider(this).get(DefinitionsVMWrapper::class.java)
        androidVM = ViewModelProvider(this).get(ArticleVMWrapper::class.java)

        val id = requireArguments().getLong(STATE_ID)
        val vmState = savedInstanceState?.getParcelable(VM_STATE) ?: ArticleVM.State(id, DefinitionsVM.State())
        val deps = (requireContext().applicationContext as AppComponentOwner).appComponent
        val component = DaggerArticleComponent.builder()
            .setDeps(deps)
            .setDefinitionsDeps(deps)
            .setVMState(vmState)
            .setDefinitionsState(vmState.definitionsState)
            .setVMWrapper(androidVM)
            .build()
        if (!androidVM.isInitialized()) {
            component.injectViewModelWrapper(androidVM)
        }
        if (!androidDefinitionsVM.isInitialized()) {
            component.injectDefinitionsViewModelWrapper(androidDefinitionsVM)
        }
        component.injectArticleFragment(this)
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
        bindDefinitionsBottomSheet(binding)
        bindDefinitions(binding)

        binding.toolbar.navigationIcon = requireContext().getDrawableCompat(R.drawable.ic_arrow_back_24)
        binding.toolbar.setNavigationOnClickListener {
            articleVM.onBackPressed()
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

        binding.text.setOnTouchListener { v, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                val x = event.x
                val y = event.y

                val offset = binding.text.getOffsetForPosition(x, y)
                val word = findWord(binding.text.text, offset)
                if (word.isNotBlank()) {
                    articleVM.onWordClicked(word)
                }
            }
            false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            articleVM.article.collect {
                when (it) {
                    // TODO: handle loading
                    is Resource.Loaded -> {
                        binding.toolbar.title = it.data.name
                        binding.text.text = it.data.text
                    }
                    else -> {}
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            articleVM.eventFlow.collect {
                handleEvent(it)
            }
        }
    }

    private fun bindDefinitionsBottomSheet(binding: FragmentArticleBinding) {
        val bottomSheetBehavior = BottomSheetBehavior.from(binding.definitionsBottomsheet)
        bottomSheetBehavior.setPeekHeight(100);
        bottomSheetBehavior.setHideable(true);

        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
//                TODO("Not yet implemented")
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
//                TODO("Not yet implemented")
            }
        });
//
//        binding.definitionsList.setOnTouchListener { v, event ->
//            val action = event.action
//            when (action) {
//                MotionEvent.ACTION_DOWN ->                         // Disallow NestedScrollView to intercept touch events.
//                    v.parent.requestDisallowInterceptTouchEvent(false)
//                MotionEvent.ACTION_UP ->                         // Allow NestedScrollView to intercept touch events.
//                    v.parent.requestDisallowInterceptTouchEvent(false)
//            }
//
//            // Handle RecyclerView touch events.
//            v.onTouchEvent(event)
//            true
//        }
    }

    private fun bindDefinitions(binding: FragmentArticleBinding) {
        binding.definitionsList.apply {
            layoutManager = LinearLayoutManager(binding.root.context, RecyclerView.VERTICAL, false)
        }

        binding.defintionsLoader.setOnTryAgainListener {
            definitionsVM.onTryAgainClicked()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            definitionsVM.definitions.collect {
                showDefinitions(it)
            }
        }
    }

    private fun showDefinitions(it: Resource<List<BaseViewItem<*>>>) {
        val binding = this.binding!!

        val errorText = definitionsVM.getErrorText(it)?.toString(requireContext())
        it.bind(binding.defintionsLoader, errorText)

        updateListAdapter(it)
    }

    private fun updateListAdapter(it: Resource<List<BaseViewItem<*>>>) {
        val binding = this.binding!!

        if (binding.definitionsList.adapter != null) {
            (binding.definitionsList.adapter as SimpleAdapter).submitList(it.data())
        } else {
            binding.definitionsList.adapter = SimpleAdapter(binder).apply {
                submitList(it.data())
            }
        }
    }

    // Helpers

    private fun findWord(
        str: CharSequence,
        anOffset: Int
    ): String {
        var offset = anOffset
        if (str.length == offset) {
            offset--
        }

        if (offset > 0 && str[offset] == ' ') {
            offset--
        }

        var startIndex = offset
        var endIndex = offset

        while (startIndex - 1 >= 0 && !isBlankChar(str[startIndex - 1])) {
            --startIndex
        }

        while (endIndex + 1 < str.length && !isBlankChar(str[endIndex + 1])) {
            ++endIndex
        }

        if (startIndex != endIndex && endIndex < str.length) {
            ++endIndex
        }

        return str.substring(startIndex, endIndex)
    }

    private fun isBlankChar(char: Char): Boolean {
        return char == ' ' || char == '\n'
    }

    private fun handleEvent(it: Event) {
        when (it) {
            is ShowDefinitionEvent -> {
                showDefinitionPopup(it.word)
            }
            is BackNavigationEvent -> {
                childFragmentManager.popBackStack()
            }
            is ErrorEvent -> {
                showError(it.text.toString(requireContext()))
            }
        }
    }

    private fun showDefinitionPopup(word: String) {

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