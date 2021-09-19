package com.aglushkov.wordteacher.desktopapp

import androidx.compose.desktop.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.Button
import androidx.compose.ui.Modifier
import com.aglushkov.wordteacher.desktopapp.features.definitions.DefinitionsUI
import com.aglushkov.wordteacher.shared.features.ChildConfiguration
import com.aglushkov.wordteacher.shared.features.TabDecomposeComponentImpl
import com.aglushkov.wordteacher.shared.features.definitions.DefinitionsDecomposeComponent
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.repository.config.ConfigRepository
import com.aglushkov.wordteacher.shared.repository.service.ConfigConnectParamsStatFile
import com.aglushkov.wordteacher.shared.repository.service.ConfigConnectParamsStatRepository
import com.aglushkov.wordteacher.shared.repository.service.ServiceRepository
import com.aglushkov.wordteacher.shared.repository.service.WordTeacherWordServiceFactory
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.service.ConfigService
import com.arkivanov.decompose.extensions.compose.jetbrains.rememberRootComponent
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.jetbrains.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.slide

val connectivityManager = ConnectivityManager()
val wordDefinitionRepository = WordDefinitionRepository(
    ServiceRepository(
        ConfigRepository(
            ConfigService("https://soniccat.ru/"),
            connectivityManager
        ),
        ConfigConnectParamsStatRepository(
            ConfigConnectParamsStatFile()
        ),
        WordTeacherWordServiceFactory()
    )
)
val componentFactory: (context: ComponentContext, configuration: ChildConfiguration) -> DefinitionsDecomposeComponent =
    { context: ComponentContext, configuration: ChildConfiguration ->
        DefinitionsDecomposeComponent(
            context,
            configuration.word,
            connectivityManager,
            wordDefinitionRepository,
            IdGenerator()
        )
    }

fun main() {
    Window("WordTeacher") {
        Surface(color = MaterialTheme.colors.background) {
            Box(modifier = Modifier.fillMaxSize()) {
                // TODO: use dagger when they fix this https://github.com/JetBrains/compose-jb/issues/1022
//                val component = rememberRootComponent {
//                    DaggerDefinitionsComposeComponent.builder()
//                        .setComponentContext(it)
//                        .setWord(null)
//                        .setDeps(deps)
//                        .build()
//                        .rootDecomposeComponent()
//                }

                val component = rememberRootComponent {
                    TabDecomposeComponentImpl(
                        it,
                        componentFactory
                    )
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
