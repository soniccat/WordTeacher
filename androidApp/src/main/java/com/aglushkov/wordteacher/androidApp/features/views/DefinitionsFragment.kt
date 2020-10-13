package com.aglushkov.wordteacher.androidApp.features.views

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aglushkov.wordteacher.androidApp.GApp
import com.aglushkov.wordteacher.androidApp.databinding.FragmentDefinitionsBinding
import com.aglushkov.wordteacher.androidApp.features.definitions.DefinitionsAdapter
import com.aglushkov.wordteacher.androidApp.features.definitions.DefinitionsBinder
import com.aglushkov.wordteacher.androidApp.general.views.bind
import com.aglushkov.wordteacher.di.AppComponentOwner
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsDisplayMode
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import dev.icerock.moko.mvvm.utils.bind

class DefinitionsVMWrapper(
    application: Application,
    saveState: SavedStateHandle
): AndroidViewModel(application) {

    // TODO: fix DI mess
    val vm: DefinitionsVM = DefinitionsVM(
        (application as AppComponentOwner).appComponent.getConnectivityManager(),
        (application as AppComponentOwner).appComponent.getWordRepository(),
        saveState.get<DefinitionsVM.State>("state") ?: DefinitionsVM.State()
    )

    init {

    }
}

class DefinitionsFragment: Fragment() {
    private lateinit var vm: DefinitionsVM
    private var binding: FragmentDefinitionsBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        vm = ViewModelProviders.of(this, SavedStateViewModelFactory(requireActivity().application, this))
                .get(DefinitionsVMWrapper::class.java).vm
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwnerLiveData.observe(this, Observer {
            if (it == null) return@Observer
            onViewLifecycleOwnerReady(it)
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDefinitionsBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindView()
    }

    private fun bindView() {
        val binding = this.binding!!

        binding.list.apply {
            layoutManager = LinearLayoutManager(binding.root.context, RecyclerView.VERTICAL, false)
        }

        binding.loadingStatusView.setOnTryAgainListener {
            vm.onTryAgainClicked()
        }

        binding.searchview.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                vm.onWordSubmitted(query ?: "")
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

    private fun onViewLifecycleOwnerReady(viewLifecycleOwner: LifecycleOwner) {
        vm.definitions.bind(viewLifecycleOwner) {
            showDefinitions(it!!)
        }

//        vm.definitions.observe(viewLifecycleOwner, Observer {
//            showDefinitions(it)
//        })
    }

    private fun showDefinitions(it: Resource<List<BaseViewItem<*>>>) {
        val binding = this.binding!!

        val errorText = vm.getErrorText(it)?.toString(requireContext())
        it.bind(binding.loadingStatusView, errorText)

        updateListAdapter(it)
    }

    private fun updateListAdapter(it: Resource<List<BaseViewItem<*>>>) {
        val binding = this.binding!!

        if (binding.list.adapter != null) {
            (binding.list.adapter as DefinitionsAdapter).submitList(it.data())
        } else {
            val binder = DefinitionsBinder()
            binder.listener = object : DefinitionsBinder.Listener {
                override fun onDisplayModeChanged(mode: DefinitionsDisplayMode) {
                    vm.onDisplayModeChanged(mode)
                }
            }

            binding.list.adapter = DefinitionsAdapter(binder).apply {
                submitList(it.data())
            }
        }
    }
}