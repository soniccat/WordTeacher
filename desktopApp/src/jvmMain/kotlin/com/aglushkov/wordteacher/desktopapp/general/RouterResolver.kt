package com.aglushkov.wordteacher.desktopapp.general

import com.aglushkov.wordteacher.desktopapp.Router
import com.aglushkov.wordteacher.desktopapp.di.AppComp
import java.lang.ref.WeakReference
import javax.inject.Inject

@AppComp
class RouterResolver @Inject constructor() {
    private val privateRouter = object : Router {
    }
    var router: WeakReference<Router>? = WeakReference(privateRouter)


    fun attach() {
    }

    fun detach() {
    }
}