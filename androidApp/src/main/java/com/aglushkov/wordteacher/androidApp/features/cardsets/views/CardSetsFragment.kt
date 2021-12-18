package com.aglushkov.wordteacher.androidApp.features.cardsets.views

import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.databinding.FragmentArticlesBinding
import com.aglushkov.wordteacher.androidApp.databinding.FragmentCardsetsBinding
import com.aglushkov.wordteacher.androidApp.features.cardsets.di.DaggerCardSetsComponent
import com.aglushkov.wordteacher.androidApp.general.AndroidVM
import com.aglushkov.wordteacher.androidApp.general.ViewItemBinder
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveThemeColor
import com.aglushkov.wordteacher.androidApp.general.extensions.submit
import com.aglushkov.wordteacher.androidApp.general.views.bind
import com.aglushkov.wordteacher.di.AppComponentOwner
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsVM
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

class CardSetsAndroidVM(
    application: Application
): AndroidVM<CardSetsVM>(application)

class CardSetsFragment: Fragment() {
    private lateinit var androidVM: CardSetsAndroidVM
    private lateinit var cardSetsVM: CardSetsVM
    private var binding: FragmentCardsetsBinding? = null

    @Inject lateinit var binder: ViewItemBinder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidVM = ViewModelProvider(this)
                .get(CardSetsAndroidVM::class.java)

        //val vmState = savedInstanceState?.getParcelable(VM_STATE) ?: DefinitionsVM.State()
        val deps = (requireContext().applicationContext as AppComponentOwner).appComponent
        val component = DaggerCardSetsComponent.builder()
            .setDeps(deps)
            //.setVMState(vmState)
            .setVMWrapper(androidVM)
            .build()
        if (!androidVM.isInitialized()) {
            component.injectViewModelWrapper(androidVM)
        }
        component.injectCardSetsFragment(this)

        cardSetsVM = androidVM.vm
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        //outState.putParcelable(VM_STATE, definitionsVM.state)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCardsetsBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindView()

        viewLifecycleOwner.lifecycleScope.launch {
            cardSetsVM.cardSets.collect {
                showCardSets(it)
            }
        }
    }

    private fun bindView() {
        val binding = this.binding!!

        binding.list.apply {
            layoutManager = LinearLayoutManager(binding.root.context, RecyclerView.VERTICAL, false)
        }

        val context = requireContext()
        binding.startLearningButton.setOnClickListener {
            cardSetsVM.onStartLearningClicked()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun showCardSets(it: Resource<List<BaseViewItem<*>>>) {
        val binding = this.binding!!

        it.bind(binding.loadingStatusView)

        updateListAdapter(it)
    }

    private fun updateListAdapter(it: Resource<List<BaseViewItem<*>>>) =
        binding!!.list.submit(it, binder)
}

private val VM_STATE = "vm_state"
