package com.altair.app

data class SavedPlace(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val radiusM: Double = 30.0,
    val createdAtMs: Long = System.currentTimeMillis()
)

data class PlaceStats(
    val samples: Int,
    val avgRsrp: Double?,        // dBm, if available
    val avgRsrq: Double?,        // dB, if available
    val avgRssi: Double?,        // dBm, if available
    val qualityLabel: String,
    val qualityScoreDbm: Double? // Value used as quality reference: RSRP or RSSI
)