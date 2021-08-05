package com.aglushkov.wordteacher.desktopapp

import androidx.compose.desktop.DesktopTheme
import androidx.compose.desktop.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.arkivanov.decompose.extensions.compose.jetbrains.rememberRootComponent

fun main() {
    Window("WordTeacher") {
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
