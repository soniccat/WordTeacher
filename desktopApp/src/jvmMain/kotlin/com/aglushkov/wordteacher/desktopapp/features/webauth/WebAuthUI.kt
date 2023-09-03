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
                JFXWebView {
                    it.load(vm.initialUrl.toString())
                    it.onLocationChanged = { newUrl ->
                        vm.onUrlChanged(newUrl)
                    }
                }
            }
        )
        Text("webview")
    }
}

class JFXWebView(
    val initializedCallback: (JFXWebView) -> Unit,
) : JFXPanel() {
    private var webView: WebView? = null
    var onLocationChanged: (String) -> Unit = {}

    init {
        Platform.runLater(::initialiseJavaFXScene)
    }

    private fun initialiseJavaFXScene() {
        Platform.setImplicitExit(false)
        webView = WebView().apply {
            engine.locationProperty().addListener { observable, oldValue, newValue ->
                onLocationChanged(newValue)
            }
        }
        val scene = Scene(webView)
        setScene(scene)
        initializedCallback(this)
    }

    fun load(url: String) {
        webView?.engine?.load(url)
    }
}