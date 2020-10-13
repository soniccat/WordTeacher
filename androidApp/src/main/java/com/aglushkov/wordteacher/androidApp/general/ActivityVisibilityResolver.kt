package com.aglushkov.wordteacher.androidApp.general

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.util.concurrent.atomic.AtomicInteger

class ActivityVisibilityResolver(private val application: Application) {
    var listener: Listener? = null

    interface Listener {
        fun onFirstActivityStarted()
        fun onLastActivityStopped()
    }

    private val startedActivity = AtomicInteger()
    private val callback = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityPaused(activity: Activity?) {
        }

        override fun onActivityResumed(activity: Activity?) {
        }

        override fun onActivityStarted(activity: Activity?) {
            val value = startedActivity.incrementAndGet()
            if (value == 1) {
                listener?.onFirstActivityStarted()
            }
        }

        override fun onActivityDestroyed(activity: Activity?) {
        }

        override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
        }

        override fun onActivityStopped(activity: Activity?) {
            val value = startedActivity.decrementAndGet()
            if (value == 0) {
                listener?.onLastActivityStopped()
            }
        }

        override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
        }
    }

    fun attach() {
        application.registerActivityLifecycleCallbacks(callback)
    }

    fun deatach() {
        application.unregisterActivityLifecycleCallbacks(callback)
    }
}