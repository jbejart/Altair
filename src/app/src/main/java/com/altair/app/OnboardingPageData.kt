package com.altair.app

data class OnboardingPageData(
    val imageRes: Int,
    val title: String,
    val body: String,
    val showCheckbox: Boolean = false
)