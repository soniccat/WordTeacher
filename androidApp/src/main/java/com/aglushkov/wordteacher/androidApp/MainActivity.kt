package com.aglushkov.wordteacher.androidApp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.aglushkov.wordteacher.androidApp.compose.ComposeAppTheme
import com.aglushkov.wordteacher.androidApp.databinding.ActivityMainBinding
import com.aglushkov.wordteacher.androidApp.features.add_article.views.AddArticleFragment
import com.aglushkov.wordteacher.androidApp.features.article.views.ArticleFragment
import com.aglushkov.wordteacher.androidApp.features.articles.views.ArticlesFragment
import com.aglushkov.wordteacher.androidApp.features.cardsets.views.CardSetsFragment
import com.aglushkov.wordteacher.androidApp.features.definitions.di.DaggerDefinitionsComposeComponent
import com.aglushkov.wordteacher.androidApp.features.definitions.views.DefinitionsFragment
import com.aglushkov.wordteacher.androidApp.features.definitions.views.DefinitionsUI
import com.aglushkov.wordteacher.di.AppComponentOwner
import com.arkivanov.decompose.extensions.compose.jetpack.Children
import com.arkivanov.decompose.extensions.compose.jetpack.animation.child.slide
import com.arkivanov.decompose.extensions.compose.jetpack.rememberRootComponent
import kotlin.reflect.KClass

class MainActivity : AppCompatActivity(), Router {
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupComposeLayout()
        //setupViewLayout()
    }

    private fun setupComposeLayout() {
        val deps = (applicationContext as AppComponentOwner).appComponent

        setContent {
            ComposeAppTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val component = rememberRootComponent {
                            DaggerDefinitionsComposeComponent.builder()
                                .setComponentContext(it)
                                .setWord(null)
                                .setDeps(deps)
                                .build()
                                .rootDecomposeComponent()
                        }

                        Column {
                            Button(onClick = {
                                component.onNextChild()
                            }) {
                                Text("Open Next")
                            }
                            Children(routerState = component.routerState, animation = slide()) {
                                DefinitionsUI(it.instance.inner)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupViewLayout() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomBar.setOnNavigationItemSelectedListener {
            val cl = screenClassById(it.itemId)
            openFragment(cl)
            true
        }

        supportFragmentManager.fragmentFactory = object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                if (DefinitionsFragment::class.java.name == className) {
                    return DefinitionsFragment()
                } else if (ArticlesFragment::class.java.name == className) {
                    return ArticlesFragment()
                } else if (AddArticleFragment::class.java.name == className) {
                    return AddArticleFragment()
                } else if (ArticlesFragment::class.java.name == className) {
                    return ArticlesFragment()
                }

                return super.instantiate(classLoader, className)
            }
        }
        supportFragmentManager.addOnBackStackChangedListener {
            supportFragmentManager.fragments.lastOrNull()?.let {
                screenIdByClass(it::class)?.let { itemId ->
                    binding.bottomBar.selectedItemId = itemId
                }
            }
        }

        if (supportFragmentManager.fragments.size == 0) {
            openFragment(DefinitionsFragment::class)
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount == 1) {
            finish()
        } else {
            super.onBackPressed()
        }
    }

    private fun openFragment(cl: KClass<*>, arguments: Bundle? = null, isFullscreen: Boolean = false) {
        val tag = screenNameByClass(cl)
        val fragment = supportFragmentManager.findFragmentByTag(tag)
        val topFragment = supportFragmentManager.fragments.lastOrNull()
        if (fragment == null) {
            val newFragment = supportFragmentManager.fragmentFactory.instantiate(
                classLoader,
                cl.java.name
            )
            newFragment.arguments = arguments

            val container = if (isFullscreen) binding.fragmentContainerFullscreen else binding.fragmentContainer
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .addToBackStack(tag)
                .replace(container.id, newFragment, tag)
                .commitAllowingStateLoss()

        } else if (topFragment == null || topFragment::class != cl) {
            supportFragmentManager.popBackStack(tag, 0)
        }
    }

    private fun screenClassById(id: Int): KClass<*> = when(id) {
        R.id.tab_definitions -> DefinitionsFragment::class
        R.id.tab_articles -> ArticlesFragment::class
        R.id.tab_cardsets -> CardSetsFragment::class
        else -> throw IllegalArgumentException("Wrong screen id $id")
    }

    private fun screenIdByClass(cl: KClass<*>): Int? = when(cl) {
        DefinitionsFragment::class -> R.id.tab_definitions
        ArticlesFragment::class -> R.id.tab_articles
        CardSetsFragment::class -> R.id.tab_cardsets
        else -> null
    }

    private fun screenNameByClass(cl: KClass<*>): String = when(cl) {
        DefinitionsFragment::class -> "definitions"
        ArticlesFragment::class -> "articles"
        AddArticleFragment::class -> "addArticle"
        ArticleFragment::class -> "article"
        CardSetsFragment::class -> "cardsets"
        else -> throw IllegalArgumentException("Wrong screen class $cl")
    }

    private fun openDialogFragment(cl: KClass<*>) {
        val tag = screenNameByClass(cl)
        val newFragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader,
            cl.java.name
        ) as DialogFragment

        newFragment.show(supportFragmentManager, tag)
    }

    // Router

    override fun openAddArticle() {
        openDialogFragment(AddArticleFragment::class)
    }

    override fun openArticle(id: Long) {
        openFragment(ArticleFragment::class, ArticleFragment.createArguments(id), true)
    }

    override fun closeArticle() {
        supportFragmentManager.popBackStack()
    }

    override fun openCardSet(id: Long) {
        // TODO: need to support
        // openFragment(CardSetFragment::class, CardSetFragment.createArguments(id), true)
    }
}
