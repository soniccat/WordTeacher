package com.aglushkov.wordteacher.android_app.general

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.aglushkov.wordteacher.android_app.Router
import com.aglushkov.wordteacher.android_app.di.AppComp
import java.lang.ref.WeakReference
import javax.inject.Inject

// TODO: rethink the solution as we've switched to compose already
@AppComp
class RouterResolver @Inject constructor(private val application: Application) {
    var router: WeakReference<Router>? = null

    private val callback = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityPaused(activity: Activity) {
        }

        override fun onActivityResumed(activity: Activity) {
        }

        override fun onActivityStarted(activity: Activity) {
            if (activity is Router) {
                router = WeakReference(activity)
            }
        }

        override fun onActivityDestroyed(activity: Activity) {
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        }

        override fun onActivityStopped(activity: Activity) {
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        }
    }

    fun attach() {
        application.registerActivityLifecycleCallbacks(callback)
    }

    fun detach() {
        application.unregisterActivityLifecycleCallbacks(callback)
    }

    fun setRouter(router: Router) {
        this.router = WeakReference(router)
    }
}