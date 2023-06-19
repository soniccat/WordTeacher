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
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.v
import io.ktor.http.*
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
        webEngine.locationProperty().addListener { observable, oldValue, newValue ->
            Logger.v("web location: $newValue")

            try {
                val url = Url(newValue)
                if (url.host == "example.com") {
                    val code = url.parameters["code"].orEmpty()
                    val error = url.parameters["error"].orEmpty()

                }
            } catch (t: Throwable) {
            }
        }
        webEngine.load("https://accounts.google.com/o/oauth2/v2/auth?client_id=166526384655-9ji25ddl02vg3d91g8vc2tbvbupl6o3k.apps.googleusercontent.com&redirect_uri=https%3A%2F%2Fexample.com&scope=profile&response_type=code")
        val scene = Scene(webView)
        setScene(scene)
    }
}