package com.altair.app

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

fun AppCompatActivity.applySystemBarsInsets(root: View, bottomDock: View? = null) {
    WindowCompat.setDecorFitsSystemWindows(window, false)

    // Guardamos paddings originales (ej. tu 24dp del onboarding)
    val startLeft = root.paddingLeft
    val startTop = root.paddingTop
    val startRight = root.paddingRight
    val startBottom = root.paddingBottom

    val dockStartBottom = bottomDock?.paddingBottom ?: 0

    ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
        val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())

        // root: respeta tu padding base + insets
        v.updatePadding(
            left = startLeft + sys.left,
            top = startTop + sys.top,
            right = startRight + sys.right,
            bottom = startBottom // (normalmente 0 en home)
        )

        // bottomDock (bottomNav o footer): sube contenido sobre nav-bar
        bottomDock?.updatePadding(bottom = dockStartBottom + sys.bottom)

        insets
    }

    ViewCompat.requestApplyInsets(root)
}
