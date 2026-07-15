package com.altair.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var dotsContainer: LinearLayout
    private lateinit var btnNextOrStart: Button

    private lateinit var adapter: OnboardingAdapter
    private var termsAccepted = false

    // Screen data
    private val pages = listOf(
        OnboardingPageData(
            imageRes = R.drawable.altair_logo,
            title = "Real-world coverage",
            body = "Altair measures signal strength (RSRP), signal quality (SINR), and cell band information around you."
        ),
        OnboardingPageData(
            imageRes = R.drawable.altair_logo,
            title = "Technical geolocation",
            body = "We store latitude and longitude and securely upload them to build network maps and support academic analysis."
        ),
        OnboardingPageData(
            imageRes = R.drawable.altair_logo,
            title = "Responsible use",
            body = "You agree that we may collect radio data, approximate GPS location, and upload samples to the cloud for research purposes.",
            showCheckbox = true
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        applySystemBarsInsets(
            findViewById(R.id.rootOnboarding),
            findViewById(R.id.footerContainer)
        )

        viewPager = findViewById(R.id.viewPagerOnboarding)
        dotsContainer = findViewById(R.id.dotsContainer)
        btnNextOrStart = findViewById(R.id.btnNextOrStart)

        adapter = OnboardingAdapter(pages) { accepted ->
            termsAccepted = accepted
            updateButtonState()
        }

        viewPager.adapter = adapter

        setupDots()
        updateDots(0)
        updateButtonText(0)
        updateButtonState()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateDots(position)
                updateButtonText(position)
                updateButtonState()
            }
        })

        btnNextOrStart.setOnClickListener {
            val position = viewPager.currentItem
            val lastIndex = pages.size - 1

            if (position < lastIndex) {
                viewPager.currentItem = position + 1
            } else {
                if (termsAccepted) {
                    finishOnboardingAndOpenHome()
                }
            }
        }
    }

    private fun finishOnboardingAndOpenHome() {
        Prefs.setOnboardingDone(this)
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    // ---------------------
    // Dots indicators
    // ---------------------
    private fun setupDots() {
        dotsContainer.removeAllViews()

        repeat(pages.size) {
            val dot = android.view.View(this).apply {
                layoutParams = LinearLayout.LayoutParams(12.dp, 12.dp).apply {
                    setMargins(6.dp, 0, 6.dp, 0)
                }

                background = ContextCompat.getDrawable(
                    this@OnboardingActivity,
                    android.R.drawable.presence_invisible
                )
            }

            dotsContainer.addView(dot)
        }
    }

    private fun updateDots(position: Int) {
        for (index in 0 until dotsContainer.childCount) {
            val dotView = dotsContainer.getChildAt(index)
            val params = dotView.layoutParams as LinearLayout.LayoutParams

            if (index == position) {
                params.width = 12.dp
                params.height = 12.dp

                dotView.background = ContextCompat.getDrawable(
                    this,
                    android.R.drawable.presence_online
                )
            } else {
                params.width = 8.dp
                params.height = 8.dp

                dotView.background = ContextCompat.getDrawable(
                    this,
                    android.R.drawable.presence_invisible
                )
            }

            dotView.layoutParams = params
        }
    }

    private fun updateButtonText(position: Int) {
        val lastIndex = pages.size - 1
        btnNextOrStart.text = if (position == lastIndex) "Start" else "Next"
    }

    private fun updateButtonState() {
        val lastIndex = pages.size - 1
        val isLastPage = viewPager.currentItem == lastIndex

        btnNextOrStart.isEnabled = !isLastPage || termsAccepted
        btnNextOrStart.alpha = if (btnNextOrStart.isEnabled) 1f else 0.4f
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}
