package com.altair.app

import android.content.Context

object ServiceStateStore {

    private const val PREFS_NAME = "service_state"
    private const val KEY_MEASUREMENT_RUNNING = "measurement_running"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setMeasurementRunning(context: Context, running: Boolean) {
        prefs(context).edit()
            .putBoolean(KEY_MEASUREMENT_RUNNING, running)
            .apply()
    }

    fun isMeasurementRunning(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_MEASUREMENT_RUNNING, false)
    }

}
