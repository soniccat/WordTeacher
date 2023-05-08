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
import com.aglushkov.wordteacher.desktopapp.di.DaggerAppComponent
import com.aglushkov.wordteacher.desktopapp.features.definitions.DefinitionsUI
import com.aglushkov.wordteacher.shared.features.definitions.di.DaggerDefinitionsComposeComponent
import com.aglushkov.wordteacher.shared.features.definitions.di.DefinitionsComposeComponent
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.parcelable.ParcelableContainer
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

fun main() = application {
    val lifecycle = LifecycleRegistry()
    val stateKeeper = StateKeeperDispatcher(tryRestoreStateFromFile())
    val decomposeContext = DefaultComponentContext(
        lifecycle = lifecycle,
        stateKeeper = stateKeeper,
    )

//    val root =
//        runOnUiThread {
//            DefaultRootComponent(
//                componentContext = ,
//                featureInstaller = DefaultFeatureInstaller,
//            )
//        }

    val appComponent = DaggerAppComponent.builder()
//        .generalModule(GeneralModule())
        .build()

    val definitionsDecomposeComponent = DaggerDefinitionsComposeComponent.builder()
        .setComponentContext(decomposeContext)
        .setConfiguration(
            DefinitionsComposeComponent.DefinitionConfiguration(
                word = "fox"
            )
        )
        .setDeps(appComponent)
        .build()
        .definitionsDecomposeComponent()

    Window(onCloseRequest = ::exitApplication) {
        Surface(color = MaterialTheme.colors.background) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column {
//                    Button(onClick = {
//                        //component.onNextChild()
//                    }) {
//                        Text("Open Next")
//                    }
//                    Children(routerState = component.routerState, animation = slide()) { it ->
//                        val instance = it.instance
//                        when (instance) {
//                            is TabDecomposeComponent.Child.Definitions -> DefinitionsUI(
//                                vm = instance.inner//,
//                                //modalModifier = Modifier.padding(innerPadding)
//                            )
//                        }
//                    }
                    DefinitionsUI(definitionsDecomposeComponent)
                }
            }
        }
    }
}

private const val SAVED_STATE_FILE_NAME = "saved_state.dat"

private fun saveStateToFile(state: ParcelableContainer) {
    ObjectOutputStream(File(SAVED_STATE_FILE_NAME).outputStream()).use { output ->
        output.writeObject(state)
    }
}

private fun tryRestoreStateFromFile(): ParcelableContainer? =
    File(SAVED_STATE_FILE_NAME).takeIf(File::exists)?.let { file ->
        try {
            ObjectInputStream(file.inputStream()).use(ObjectInputStream::readObject) as ParcelableContainer
        } catch (e: Exception) {
            null
        } finally {
            file.delete()
        }
    }
