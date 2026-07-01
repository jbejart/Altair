package com.altair.app

import android.os.Build
import android.os.SystemClock
import android.telephony.CellInfo
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr

object RadioKpiUtils {

    // =========================
    // Rangos válidos aproximados
    // =========================

    fun validRsrp(value: Int?): Int? {
        if (value == null) return null
        if (value == CellInfo.UNAVAILABLE || value == Int.MAX_VALUE || value == -999) return null
        return if (value in -150..-30) value else null
    }

    fun validRsrq(value: Int?): Int? {
        if (value == null) return null
        if (value == CellInfo.UNAVAILABLE || value == Int.MAX_VALUE || value == -999) return null
        return if (value in -40..0) value else null
    }

    fun validRssi(value: Int?): Int? {
        if (value == null) return null
        if (value == CellInfo.UNAVAILABLE || value == Int.MAX_VALUE || value == -999) return null
        return if (value in -130..-20) value else null
    }

    fun validSinr(value: Double?): Double? {
        if (value == null) return null
        if (value.isNaN() || value.isInfinite()) return null
        return if (value in -30.0..50.0) value else null
    }

    // =========================
    // SINR LTE / NR
    // =========================

    fun lteSinrDbOrNull(s: CellSignalStrengthLte): Double? {
        return try {
            val raw = s.rssnr
            if (raw == CellInfo.UNAVAILABLE || raw == Int.MAX_VALUE) return null

            // LTE RSSNR normalmente viene en décimas de dB.
            val db = raw / 10.0
            validSinr(db)
        } catch (_: Throwable) {
            null
        }
    }

    fun nrSinrDbOrNull(s: CellSignalStrengthNr): Double? {
        return try {
            val raw = s.ssSinr
            if (raw == CellInfo.UNAVAILABLE || raw == Int.MAX_VALUE) return null

            // En NR normalmente Android reporta SS-SINR en dB.
            validSinr(raw.toDouble())
        } catch (_: Throwable) {
            null
        }
    }

    // =========================
    // RSSI LTE
    // =========================

    fun lteRssiDbmOrNull(s: CellSignalStrengthLte): Int? {
        return try {
            validRssi(s.rssi)
        } catch (_: Throwable) {
            null
        }
    }

    // =========================
    // Edad del CellInfo
    // =========================

    fun cellInfoAgeMs(cell: CellInfo): Long? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val ts = cell.timestampMillis
                if (ts > 0) {
                    SystemClock.elapsedRealtime() - ts
                } else {
                    null
                }
            } else {
                @Suppress("DEPRECATION")
                val tsNanos = cell.timeStamp
                if (tsNanos > 0) {
                    (SystemClock.elapsedRealtimeNanos() - tsNanos) / 1_000_000L
                } else {
                    null
                }
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun isCellInfoTooOld(ageMs: Long?, maxAgeMs: Long = 60_000L): Boolean {
        if (ageMs == null) return false
        return ageMs > maxAgeMs
    }

    // =========================
    // Firma para detectar repetidos
    // =========================

    fun buildRadioSignature(
        networkType: String?,
        pci: Any?,
        earfcn: Any?,
        nci: Any?,
        cellId: Any?,
        rsrp: Any?,
        rsrq: Any?,
        rssi: Any?,
        sinr: Any?
    ): String {
        return listOf(
            networkType ?: "",
            pci?.toString() ?: "",
            earfcn?.toString() ?: "",
            nci?.toString() ?: "",
            cellId?.toString() ?: "",
            rsrp?.toString() ?: "",
            rsrq?.toString() ?: "",
            rssi?.toString() ?: "",
            sinr?.toString() ?: ""
        ).joinToString("|")
    }

    fun hasAnyMainKpi(
        rsrp: Int?,
        rsrq: Int?,
        rssi: Int?,
        sinr: Double?
    ): Boolean {
        return rsrp != null || rsrq != null || rssi != null || sinr != null
    }
}
