package com.aglushkov.wordteacher.androidApp.features.definitions.views

import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aglushkov.wordteacher.androidApp.databinding.FragmentDefinitionsBinding
import com.aglushkov.wordteacher.androidApp.general.VMWrapper
import com.aglushkov.wordteacher.androidApp.general.ViewItemBinder
import com.aglushkov.wordteacher.androidApp.general.extensions.submit
import com.aglushkov.wordteacher.androidApp.general.views.bind
import com.aglushkov.wordteacher.androidApp.general.views.chooser_dialog.ChooserDialog
import com.aglushkov.wordteacher.androidApp.general.views.chooser_dialog.ChooserViewItem
import com.aglushkov.wordteacher.di.AppComponentOwner
import com.aglushkov.wordteacher.di.DaggerDefinitionsComponent
import com.aglushkov.wordteacher.di.DefinitionsBinder
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.ShowPartsOfSpeechFilterEvent
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

class DefinitionsVMWrapper(
    application: Application
): VMWrapper<DefinitionsVM>(application)

class DefinitionsFragment: Fragment() {
    private lateinit var androidVM: DefinitionsVMWrapper
    private lateinit var definitionsVM: DefinitionsVM
    private var binding: FragmentDefinitionsBinding? = null

    @Inject @DefinitionsBinder lateinit var binder: ViewItemBinder
    @Inject lateinit var idGenerator: IdGenerator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidVM = ViewModelProvider(this)
                .get(DefinitionsVMWrapper::class.java)

        val vmState = savedInstanceState?.getParcelable(VM_STATE) ?: DefinitionsVM.State()
        val deps = (requireContext().applicationContext as AppComponentOwner).appComponent
        val component = DaggerDefinitionsComponent.builder()
            .setDeps(deps)
            .setVMState(vmState)
            .setVMWrapper(androidVM)
            .build()
        if (!androidVM.isInitialized()) {
            component.injectViewModelWrapper(androidVM)
        }
        component.injectDefinitionsFragment(this)

        definitionsVM = androidVM.vm
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(VM_STATE, definitionsVM.state)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentDefinitionsBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindView()

        viewLifecycleOwner.lifecycleScope.launch {
            definitionsVM.definitions.collect {
                showDefinitions(it)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            definitionsVM.eventFlow.collect {
                when (it) {
                    is ShowPartsOfSpeechFilterEvent -> {
                        ChooserDialog(
                            requireContext()
                        ) { options ->
                            definitionsVM.onPartOfSpeechFilterUpdated(
                                options.filter {
                                    option -> option.isSelected
                                }.map {
                                    option -> option.obj as WordTeacherWord.PartOfSpeech
                                }
                            )
                        }.apply {
                            show()
                            showOptions(it.partsOfSpeech.map { partOfSpeech ->
                                val isSelected = it.selectedPartsOfSpeech.contains(partOfSpeech)
                                ChooserViewItem(idGenerator.nextId(), partOfSpeech.name, partOfSpeech, isSelected)
                            })
                        }
                    }
                }
            }
        }
    }

    private fun bindView() {
        val binding = this.binding!!

        binding.list.apply {
            layoutManager = LinearLayoutManager(binding.root.context, RecyclerView.VERTICAL, false)
        }

        binding.loadingStatusView.setOnTryAgainListener {
            definitionsVM.onTryAgainClicked()
        }

        binding.searchview.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                definitionsVM.onWordSubmitted(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun showDefinitions(it: Resource<List<BaseViewItem<*>>>) {
        val binding = this.binding!!

        val errorText = definitionsVM.getErrorText(it)?.toString(requireContext())
        it.bind(binding.loadingStatusView, errorText)

        updateListAdapter(it)
    }

    private fun updateListAdapter(it: Resource<List<BaseViewItem<*>>>) =
        binding!!.list.submit(it, binder)
}

private val VM_STATE = "vm_state"