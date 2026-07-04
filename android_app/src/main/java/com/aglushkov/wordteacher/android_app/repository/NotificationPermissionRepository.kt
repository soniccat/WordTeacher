package com.aglushkov.wordteacher.android_app.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.aglushkov.wordteacher.shared.general.extensions.updateWithLoadedData
import com.aglushkov.wordteacher.shared.general.extensions.waitUntilDone
import com.aglushkov.wordteacher.shared.general.resource.SimpleResourceRepository

class NotificationPermissionRepository(
    private val context: Context,
): SimpleResourceRepository<Boolean, Unit>() {
    private var requestPermissionLauncher: ActivityResultLauncher<String>? = null

    fun bind(activity: ComponentActivity) {
        requestPermissionLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            stateFlow.updateWithLoadedData(granted)
        }
    }

    override suspend fun loadInternal(arg: Unit): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                true
            } else {
                requestPermissionLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
                stateFlow.waitUntilDone().data() ?: false
            }
        } else {
            true
        }
    }
}