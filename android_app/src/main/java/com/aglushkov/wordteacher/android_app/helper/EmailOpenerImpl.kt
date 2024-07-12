package com.aglushkov.wordteacher.android_app.helper

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.aglushkov.wordteacher.shared.general.EmailOpener
import java.lang.ref.WeakReference

class EmailOpenerImpl: EmailOpener {
    private var weakActivity: WeakReference<ComponentActivity>? = null

    fun bind(activity: ComponentActivity) {
        weakActivity = WeakReference(activity)
    }

    override fun open(email: String) {
        val context = weakActivity?.get() ?: return

        try {
            val intent = Intent(Intent.ACTION_SEND)
            intent.setType("plain/text");
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            ContextCompat.startActivity(context, Intent.createChooser(intent, "Send mail..."), null)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "There is no email client installed.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}