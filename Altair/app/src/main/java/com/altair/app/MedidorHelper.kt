package com.altair.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.telephony.CellInfoLte
import android.telephony.CellSignalStrengthLte
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import com.google.firebase.firestore.FirebaseFirestore

object MeasurementHelper {

    private val firestore = FirebaseFirestore.getInstance()

    fun uploadSample(context: Context, location: Location?, rsrp: Int?) {
        if (location == null) return

        val sample = hashMapOf(
            "timestampMs" to System.currentTimeMillis(),
            "lat" to location.latitude,
            "lon" to location.longitude,
            "rsrpDbm" to (rsrp ?: -999)
        )

        firestore.collection("measurements")
            .add(sample)
    }

    fun getRsrp(context: Context): Int? {
        if (
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        val cellInfoList = telephonyManager.allCellInfo ?: return null

        val lteCell = cellInfoList
            .filterIsInstance<CellInfoLte>()
            .firstOrNull()

        val signalStrength = lteCell?.cellSignalStrength as? CellSignalStrengthLte

        return signalStrength?.rsrp
    }
}