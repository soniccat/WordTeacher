package com.aglushkov.wordteacher.desktopapp.features.webauth

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.aglushkov.wordteacher.shared.features.Cancelled
import com.aglushkov.wordteacher.shared.features.webauth.vm.WebAuthVM
import com.aglushkov.wordteacher.shared.general.CustomDialogUI

@Composable
fun WebAuthUI(vm: WebAuthVM, onCompleted: () -> Unit) {
    CustomDialogUI(onDismissRequest = {
        vm.onError(Cancelled)
        onCompleted()
    }) {
        Text("webview")
    }
}