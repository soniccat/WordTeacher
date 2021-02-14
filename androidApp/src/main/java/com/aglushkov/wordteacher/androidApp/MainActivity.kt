package com.aglushkov.wordteacher.androidApp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.aglushkov.wordteacher.androidApp.databinding.ActivityMainBinding
import com.aglushkov.wordteacher.androidApp.features.articles.views.ArticlesFragment
import com.aglushkov.wordteacher.androidApp.features.definitions.views.DefinitionsFragment
import java.lang.IllegalArgumentException
import kotlin.reflect.KClass

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomBar.setOnNavigationItemSelectedListener {
            val cl = screenClassById(it.itemId)
            openFragment(cl)
            true
        }

        binding.bottomBar.setOnNavigationItemReselectedListener {
            // do nothing
        }

        supportFragmentManager.fragmentFactory = object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                if (DefinitionsFragment::class.java.name == className) {
                    return DefinitionsFragment()
                } else if (ArticlesFragment::class.java.name == className) {
                    return ArticlesFragment()
                }

                return super.instantiate(classLoader, className)
            }
        }
        supportFragmentManager.addOnBackStackChangedListener {
            supportFragmentManager.fragments.lastOrNull()?.let {
                val itemId = screenIdByClass(it::class)
                binding.bottomBar.selectedItemId = itemId
            }
        }

        if (supportFragmentManager.fragments.size == 0) {
            openFragment(DefinitionsFragment::class)
        }
    }

    private fun openFragment(cl: KClass<*>) {
        val tag = screenNameByClass(cl)
        val fragment = supportFragmentManager.findFragmentByTag(tag)
        val topFragment = supportFragmentManager.fragments.lastOrNull()
        if (fragment == null) {
            val newFragment = supportFragmentManager.fragmentFactory.instantiate(classLoader, cl.java.name)
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true).apply {
                    if (topFragment != null) {
                        addToBackStack(tag)
                    }
                }
                .replace(binding.fragmentContainer.id, newFragment, tag)
                .commitAllowingStateLoss()

        } else if (topFragment == null || topFragment::class != cl) {
            if (supportFragmentManager.backStackEntryCount == 1) {
                supportFragmentManager.popBackStack()
            } else {
                supportFragmentManager.popBackStack(tag, 0)
            }
        }
    }

    private fun screenClassById(id: Int): KClass<*> = when(id) {
        R.id.tab_definitions -> DefinitionsFragment::class
        R.id.tab_articles -> ArticlesFragment::class
        else -> throw IllegalArgumentException("Wrong screen id $id")
    }

    private fun screenIdByClass(cl: KClass<*>): Int = when(cl) {
        DefinitionsFragment::class -> R.id.tab_definitions
        ArticlesFragment::class -> R.id.tab_articles
        else -> throw IllegalArgumentException("Wrong screen class $cl")
    }

    private fun screenNameByClass(cl: KClass<*>): String = when(cl) {
        DefinitionsFragment::class -> "definitions"
        ArticlesFragment::class -> "articles"
        else -> throw IllegalArgumentException("Wrong screen class $cl")
    }
}
