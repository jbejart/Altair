package com.altair.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Small visual delay so the logo can be displayed
        Handler(Looper.getMainLooper()).postDelayed({
            if (Prefs.hasSeenOnboarding(this)) {
                startActivity(Intent(this, HomeActivity::class.java))
            } else {
                startActivity(Intent(this, OnboardingActivity::class.java))
            }

            // Close splash so it does not return when pressing back
            finish()
        }, SPLASH_DELAY_MS)
    }

    companion object {
        private const val SPLASH_DELAY_MS = 1_200L
    }
}
