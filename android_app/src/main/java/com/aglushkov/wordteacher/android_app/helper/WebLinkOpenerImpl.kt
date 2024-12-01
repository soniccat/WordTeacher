package com.aglushkov.wordteacher.android_app.helper

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.aglushkov.wordteacher.android_app.R
import com.aglushkov.wordteacher.android_app.compose.DarkColorPalette
import com.aglushkov.wordteacher.android_app.compose.LightColorPalette
import com.aglushkov.wordteacher.android_app.features.textaction.TextActionActivity
import com.aglushkov.wordteacher.shared.general.WebLinkOpener
import com.aglushkov.wordteacher.shared.general.views.dpToPx
import com.aglushkov.wordteacher.shared.res.MR
import java.lang.ref.WeakReference


class WebLinkOpenerImpl: WebLinkOpener {
    private var weakActivity: WeakReference<ComponentActivity>? = null

    fun bind(activity: ComponentActivity) {
        weakActivity = WeakReference(activity)
    }

    override fun open(link: String) {
        val activity = weakActivity?.get() ?: return
        try {
            val intent = CustomTabsIntent.Builder()
                // set the default color scheme
                .setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams.Builder()
                        .setToolbarColor(LightColorPalette.primary.toArgb())
                        .build())
                // set the alternative dark color scheme
                .setColorSchemeParams(
                    CustomTabsIntent.COLOR_SCHEME_DARK,
                    CustomTabColorSchemeParams.Builder()
                        .setToolbarColor(DarkColorPalette.background.toArgb())
                        .build())
                // import button
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .setActionButton(
                    AppCompatResources.getDrawable(
                        activity,
                        R.drawable.ic_download_for_offline_24
                    ).also {
                        it?.setTint(
                            if ((activity.resources.configuration.uiMode and UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES) {
                                Color(0xFFFFFFFF).toArgb()
                            } else {
                                Color(0xFF1F1F1F).toArgb()
                            }
                        )
                    }!!.toBitmap(),
                    MR.strings.cardset_info_import_action.getString(activity),
                    PendingIntent.getActivity(
                        activity.applicationContext,
                        0,
                        Intent(
                            activity.applicationContext,
                            TextActionActivity::class.java
                        ).also {
                            it.putExtra(Intent.EXTRA_PROCESS_TEXT, link)
                        },
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                )
                .build()
            intent.launchUrl(activity, Uri.parse(link));
        } catch (e: Throwable) {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setData(Uri.parse(link))
                weakActivity?.get()?.startActivity(intent)
            } catch (e: Throwable) {
            }
        }
    }
}
