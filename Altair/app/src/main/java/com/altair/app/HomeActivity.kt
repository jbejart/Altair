package com.altair.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class HomeActivity : AppCompatActivity() {

    private lateinit var navHome: LinearLayout
    private lateinit var navStats: LinearLayout

    private lateinit var iconHome: ImageView
    private lateinit var iconStats: ImageView

    private lateinit var txtHome: TextView
    private lateinit var txtStats: TextView

    private val requestBasePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // MapFragment also validates permissions when using GPS
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        applySystemBarsInsets(
            findViewById(R.id.rootHome),
            findViewById(R.id.bottomNav)
        )

        navHome = findViewById(R.id.navHome)
        navStats = findViewById(R.id.navStats)

        iconHome = findViewById(R.id.iconHome)
        iconStats = findViewById(R.id.iconStats)

        txtHome = findViewById(R.id.txtHome)
        txtStats = findViewById(R.id.txtStats)

        requestRequiredBasePermissions()

        if (savedInstanceState == null) {
            openFragment(MapFragment())
            selectBottomNav(isHomeSelected = true)
        }

        navHome.setOnClickListener {
            openFragment(MapFragment())
            selectBottomNav(isHomeSelected = true)
        }

        navStats.setOnClickListener {
            openFragment(StatsFragment())
            selectBottomNav(isHomeSelected = false)
        }
    }

    fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.contentFrame, fragment)
            .commit()
    }

    private fun selectBottomNav(isHomeSelected: Boolean) {
        val selectedBackground = R.drawable.bg_nav_selected
        val unselectedBackground = R.drawable.bg_nav_unselected

        val selectedTextColor = ContextCompat.getColor(this, R.color.altair_text)
        val unselectedTextColor = ContextCompat.getColor(this, R.color.altair_text_secondary)
        val selectedIconColor = ContextCompat.getColor(this, R.color.altair_primary)
        val unselectedIconColor = ContextCompat.getColor(this, R.color.altair_text_secondary)

        if (isHomeSelected) {
            navHome.setBackgroundResource(selectedBackground)
            navStats.setBackgroundResource(unselectedBackground)

            txtHome.setTextColor(selectedTextColor)
            txtStats.setTextColor(unselectedTextColor)

            iconHome.setColorFilter(selectedIconColor)
            iconStats.setColorFilter(unselectedIconColor)
        } else {
            navStats.setBackgroundResource(selectedBackground)
            navHome.setBackgroundResource(unselectedBackground)

            txtStats.setTextColor(selectedTextColor)
            txtHome.setTextColor(unselectedTextColor)

            iconStats.setColorFilter(selectedIconColor)
            iconHome.setColorFilter(unselectedIconColor)
        }
    }

    private fun requestRequiredBasePermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )

        if (Build.VERSION.SDK_INT >= 33) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }

        val hasMissingPermissions = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (hasMissingPermissions) {
            requestBasePermissions.launch(permissions.toTypedArray())
        }
    }
}