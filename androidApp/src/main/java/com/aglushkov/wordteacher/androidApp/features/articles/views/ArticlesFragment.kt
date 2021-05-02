package com.aglushkov.wordteacher.androidApp.features.articles.views

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
import com.aglushkov.wordteacher.androidApp.features.articles.di.DaggerArticlesComponent
import com.aglushkov.wordteacher.androidApp.general.AndroidVM
import com.aglushkov.wordteacher.androidApp.general.ViewItemBinder
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveThemeColor
import com.aglushkov.wordteacher.androidApp.general.extensions.submit
import com.aglushkov.wordteacher.androidApp.general.views.bind
import com.aglushkov.wordteacher.di.AppComponentOwner
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesVM
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

class ArticlesAndroidVM(
    application: Application
): AndroidVM<ArticlesVM>(application)

class ArticlesFragment: Fragment() {
    private lateinit var androidVM: ArticlesAndroidVM
    private lateinit var articlesVM: ArticlesVM
    private var binding: FragmentArticlesBinding? = null

    @Inject lateinit var binder: ViewItemBinder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidVM = ViewModelProvider(this)
                .get(ArticlesAndroidVM::class.java)

        //val vmState = savedInstanceState?.getParcelable(VM_STATE) ?: DefinitionsVM.State()
        val deps = (requireContext().applicationContext as AppComponentOwner).appComponent
        val component = DaggerArticlesComponent.builder()
            .setDeps(deps)
            //.setVMState(vmState)
            .setVMWrapper(androidVM)
            .build()
        if (!androidVM.isInitialized()) {
            component.injectViewModelWrapper(androidVM)
        }
        component.injectArticlesFragment(this)

        articlesVM = androidVM.vm
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        //outState.putParcelable(VM_STATE, definitionsVM.state)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentArticlesBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindView()

        viewLifecycleOwner.lifecycleScope.launch {
            articlesVM.articles.collect {
                showArticles(it)
            }
        }
    }

    private fun bindView() {
        val binding = this.binding!!

        binding.list.apply {
            layoutManager = LinearLayoutManager(binding.root.context, RecyclerView.VERTICAL, false)
        }

        val context = requireContext()
        val buttonBgColor = context.resolveThemeColor(R.attr.colorSecondary)
        val imageColor = context.resolveThemeColor(R.attr.colorOnSecondary)
        binding.createArticleSpeedDial.addActionItem(
            SpeedDialActionItem.Builder(R.id.articles_action_add_text, R.drawable.ic_add_text_24dp)
                .setFabBackgroundColor(buttonBgColor)
                .setFabImageTintColor(imageColor)
                .setLabel(R.string.articles_action_add_text)
                .create())

        binding.createArticleSpeedDial.setOnActionSelectedListener(SpeedDialView.OnActionSelectedListener { actionItem ->
            when (actionItem.id) {
                R.id.articles_action_add_text -> {
                    articlesVM.onCreateTextArticleClicked()
                    binding.createArticleSpeedDial.close()
                    return@OnActionSelectedListener true
                }
            }
            false
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun showArticles(it: Resource<List<BaseViewItem<*>>>) {
        val binding = this.binding!!

        it.bind(binding.loadingStatusView)

        updateListAdapter(it)
    }

    private fun updateListAdapter(it: Resource<List<BaseViewItem<*>>>) =
        binding!!.list.submit(it, binder)
}

private val VM_STATE = "vm_state"
