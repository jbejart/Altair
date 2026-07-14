package com.altair.app

import android.content.Context

object Prefs {

    private const val PREFS_NAME = "altair_prefs"
    private const val KEY_ONBOARDING_DONE = "hasSeenOnboarding"

    fun hasSeenOnboarding(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(KEY_ONBOARDING_DONE, false)
    }

    fun setOnboardingDone(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putBoolean(KEY_ONBOARDING_DONE, true)
            .apply()
    }
}