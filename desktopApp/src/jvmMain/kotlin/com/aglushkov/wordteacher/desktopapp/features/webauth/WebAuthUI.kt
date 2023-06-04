package com.aglushkov.wordteacher.desktopapp.features.webauth

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import com.aglushkov.wordteacher.shared.features.Cancelled
import com.aglushkov.wordteacher.shared.features.webauth.vm.WebAuthVM
import com.aglushkov.wordteacher.shared.general.CustomDialogUI
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import javafx.application.Platform

@Composable
fun WebAuthUI(vm: WebAuthVM, onCompleted: () -> Unit) {
    CustomDialogUI(onDismissRequest = {
        vm.onError(Cancelled)
        onCompleted()
    }) {
        SwingPanel(
            background = Color.Transparent,
            modifier = Modifier.fillMaxSize(),
            factory = {
                JFXWebView()
            }
        )
        Text("webview")
    }
}

class JFXWebView : JFXPanel() {
    init {
        Platform.runLater(::initialiseJavaFXScene)
    }

    private fun initialiseJavaFXScene() {
        val webView = WebView()
        val webEngine = webView.engine
        webEngine.load("https://html5test.com/")
        val scene = Scene(webView)
        setScene(scene)
    }
}