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

        openFragment(DefinitionsFragment::class)
    }

    private fun openFragment(cl: KClass<*>) {
        val tag = screenNameByClass(cl)
        if (supportFragmentManager.findFragmentByTag(tag) == null) {
            val fragment = supportFragmentManager.fragmentFactory.instantiate(classLoader, cl.java.name)
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(binding.fragmentContainer.id, fragment, tag)
                .commitAllowingStateLoss()
        }
    }

    private fun screenClassById(id: Int): KClass<*> = when(id) {
        R.id.tab_definitions -> DefinitionsFragment::class
        R.id.tab_articles -> ArticlesFragment::class
        else -> throw IllegalArgumentException("Wrong screen id " + id)
    }

    private fun screenNameByClass(cl: KClass<*>): String = when(cl) {
        DefinitionsFragment::class -> "definitions"
        ArticlesFragment::class -> "articles"
        else -> throw IllegalArgumentException("Wrong screen class " + cl)
    }
}
