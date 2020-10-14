package com.aglushkov.wordteacher.androidApp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.aglushkov.wordteacher.androidApp.databinding.ActivityMainBinding
import com.aglushkov.wordteacher.androidApp.features.definitions.views.DefinitionsFragment

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportFragmentManager.fragmentFactory = object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                if (DefinitionsFragment::class.java.name == className) {
                    return DefinitionsFragment()
                }

                return super.instantiate(classLoader, className)
            }
        }

        if (supportFragmentManager.findFragmentByTag("definitions") == null) {
            val fragment = supportFragmentManager.fragmentFactory.instantiate(classLoader, DefinitionsFragment::class.java.name)
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(binding.fragmentContainer.id, fragment, "definitions")
                .commitAllowingStateLoss()
        }
    }
}
