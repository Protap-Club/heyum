package com.pgstorm148.heyum.firebase

import android.app.Activity

object AndroidActivityHolder {
    @Volatile
    private var activity: Activity? = null

    fun setActivity(currentActivity: Activity) {
        activity = currentActivity
    }

    fun clearActivity(currentActivity: Activity) {
        if (activity === currentActivity) {
            activity = null
        }
    }

    fun getActivity(): Activity? = activity
}
