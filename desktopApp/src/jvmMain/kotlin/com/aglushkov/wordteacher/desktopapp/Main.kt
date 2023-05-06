package com.aglushkov.wordteacher.desktopapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.Button
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.aglushkov.wordteacher.desktopapp.di.GeneralModule
import com.aglushkov.wordteacher.desktopapp.features.definitions.DefinitionsUI
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.repository.config.ConfigRepository
import com.aglushkov.wordteacher.shared.repository.service.ConfigConnectParamsStatFile
import com.aglushkov.wordteacher.shared.repository.service.ConfigConnectParamsStatRepository
import com.aglushkov.wordteacher.shared.repository.service.ServiceRepository
import com.aglushkov.wordteacher.shared.repository.service.WordTeacherWordServiceFactory
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.service.ConfigService
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.jetbrains.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.slide
import com.aglushkov.wordteacher.desktopapp.features.definitions.di.DaggerDefinitionsComposeComponent
import com.aglushkov.wordteacher.desktopapp.di.DaggerAppComponent
import com.aglushkov.wordteacher.shared.features.TabDecomposeComponent

fun main() = application {
    val appComponent = DaggerAppComponent.builder()
//        .generalModule(GeneralModule())
        .build()

    Window(onCloseRequest = ::exitApplication) {
        Surface(color = MaterialTheme.colors.background) {
            Box(modifier = Modifier.fillMaxSize()) {
                // TODO: use dagger when they fix this https://github.com/JetBrains/compose-jb/issues/1022
//                val component = rememberRootComponent {
//                    DaggerDefinitionsComposeComponent.builder()
//                        .setComponentContext(it)
//                        .setWord(null)
//                        .setDeps(appComponent)
//                        .build()
//                        .rootDecomposeComponent()
//                }

                Column {
                    Button(onClick = {
                        //component.onNextChild()
                    }) {
                        Text("Open Next")
                    }
//                    Children(routerState = component.routerState, animation = slide()) { it ->
//                        val instance = it.instance
//                        when (instance) {
//                            is TabDecomposeComponent.Child.Definitions -> DefinitionsUI(
//                                vm = instance.inner//,
//                                //modalModifier = Modifier.padding(innerPadding)
//                            )
//                        }
//                    }
                }
            }
        }
    }
}
