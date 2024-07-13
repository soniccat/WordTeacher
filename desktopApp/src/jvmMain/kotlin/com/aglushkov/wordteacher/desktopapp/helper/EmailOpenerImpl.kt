package com.aglushkov.wordteacher.desktopapp.helper

import com.aglushkov.wordteacher.shared.general.EmailOpener
import java.awt.Desktop
import java.net.URI


class EmailOpenerImpl: EmailOpener {
    override fun open(email: String) {
        if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.MAIL)) {
                val mailto = URI("mailto:$email")
                desktop.mail(mailto)
            }
        }
    }
}