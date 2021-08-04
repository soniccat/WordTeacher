package com.aglushkov.wordteacher.desktopapp

import androidx.compose.desktop.DesktopTheme
import androidx.compose.desktop.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

fun main() {
    Window("WordTeacher") {
        Surface(modifier = Modifier.fillMaxSize()) {
            MaterialTheme {
                DesktopTheme {
//                    val root = rememberRootComponent(factory = ::RootComponent)
//                    RootUi(root)
                    Box(
                        modifier = Modifier.background(color = Color.Blue)
                    ) {
                        Text(text = "Hello")
                    }
                }
            }
        }
    }
}