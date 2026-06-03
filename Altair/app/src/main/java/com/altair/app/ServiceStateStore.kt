package com.altair.app

import android.content.Context

object ServiceStateStore {

    private const val PREFS_NAME = "service_state"
    private const val KEY_LOCAL_RUNNING = "local_running"
    private const val KEY_FIREBASE_RUNNING = "firebase_running"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setLocalRunning(context: Context, running: Boolean) {
        prefs(context).edit()
            .putBoolean(KEY_LOCAL_RUNNING, running)
            .apply()
    }

    fun isLocalRunning(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_LOCAL_RUNNING, false)
    }

    fun setFirebaseRunning(context: Context, running: Boolean) {
        prefs(context).edit()
            .putBoolean(KEY_FIREBASE_RUNNING, running)
            .apply()
    }

    fun isFirebaseRunning(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_FIREBASE_RUNNING, false)
    }
}