package com.altair.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Foreground service that captures one LTE / NR radio KPI sample every 5 seconds,
 * stores it locally in CSV, and uploads that same document to Cloud Firestore.
 */
class ForegroundMeasurementService : Service() {

    companion object {
        private const val TAG = "AltairCloudService"

        private const val CHANNEL_ID = "altair_measurement_channel"
        private const val NOTIFICATION_ID = 1

        private const val CAPTURE_INTERVAL_MS = 5_000L

        private const val LOCATION_INTERVAL_MS = 4_000L
        private const val MIN_DISTANCE_M = 3f

        private const val CELLINFO_TIMEOUT_MS = 1_000L
        private const val MAX_CELLINFO_AGE_MS = 60_000L

        private const val FIRESTORE_COLLECTION = "measurements_agg"
    }

    private lateinit var telephonyManager: TelephonyManager
    private lateinit var firestore: FirebaseFirestore
    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var auth: FirebaseAuth

    @Volatile
    private var isRunning = false

    private var currentLocation: Location? = null
    private var captureThread: Thread? = null
    private var locationCallback: LocationCallback? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var lastRxBytes = 0L
    private var lastTxBytes = 0L
    private var lastTrafficTime = System.currentTimeMillis()

    private var lastRadioSignature: String? = null
    private var repeatedCount = 0

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        firestore = FirebaseFirestore.getInstance()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        auth = FirebaseAuth.getInstance()

        createNotificationChannel()
        startAsForeground()
        acquireWakeLock()
        startLocationUpdates()

        isRunning = true
        ServiceStateStore.setMeasurementRunning(this, true)
        startCaptureLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        ServiceStateStore.setMeasurementRunning(this, true)

        if (captureThread?.isAlive != true) {
            startCaptureLoop()
        }

        Log.d(TAG, "Service running")
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "Service stopped")

        isRunning = false
        captureThread?.interrupt()
        captureThread = null

        locationCallback?.let { fusedClient.removeLocationUpdates(it) }
        locationCallback = null

        releaseWakeLock()
        ServiceStateStore.setMeasurementRunning(this, false)

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Altair measurement",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Foreground service that saves radio KPI samples locally and sends them to Firebase Firestore."
        }

        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun startAsForeground() {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, HomeActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Altair measurement active")
            .setContentText("Saving and uploading one radio KPI sample every 5 seconds...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (!hasLocationPermission()) {
            Log.w(TAG, "ACCESS_FINE_LOCATION not granted - location unavailable")
            return
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_INTERVAL_MS)
            .setMinUpdateDistanceMeters(MIN_DISTANCE_M)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                currentLocation = result.lastLocation
            }
        }

        fusedClient.requestLocationUpdates(request, locationCallback!!, mainLooper)
    }

    private fun startCaptureLoop() {
        if (captureThread?.isAlive == true) return

        captureThread = Thread {
            while (isRunning) {
                val sampleStartMs = System.currentTimeMillis()

                try {
                    val document = captureSingleSample(sampleStartMs)

                    if (document != null) {
                        LocalTrackStore.appendDocument(this, document)
                        uploadToFirestore(document)
                    } else {
                        Log.w(TAG, "Invalid sample - skipping storage")
                    }

                    sleepRemainingInterval(sampleStartMs)
                } catch (_: InterruptedException) {
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Capture error: ${e.message}", e)
                    sleepCaptureIntervalQuietly()
                }
            }
        }.apply {
            name = "AltairCloudCaptureThread"
            start()
        }
    }

    private fun captureSingleSample(sampleStartMs: Long): MutableMap<String, Any>? {
        val location = currentLocation
        if (location == null) {
            Log.w(TAG, "No GPS location available")
            return null
        }

        val radioData = buildRadioData(getFreshCellInfo())
        if (radioData == null) {
            Log.w(TAG, "No radio data available")
            return null
        }

        val hasKpi = radioData["hasAnyRadioKpi"] as? Boolean ?: false
        if (!hasKpi) {
            Log.w(TAG, "No valid KPI available")
            return null
        }

        val cellInfoTooOld = radioData["cellInfoTooOld"] as? Boolean ?: false
        if (cellInfoTooOld) {
            Log.w(TAG, "CellInfo too old")
            return null
        }

        val sampleEndMs = System.currentTimeMillis()

        Log.d(
            TAG,
            "Sample captured | rsrp=${radioData["rsrpDbm"]} " +
                    "rsrq=${radioData["rsrqDb"]} " +
                    "rssi=${radioData["rssiDbm"]} " +
                    "sinr=${radioData["sinrDb"]}"
        )

        return buildSampleDocument(
            location = location,
            radioData = radioData,
            sampleStartMs = sampleStartMs,
            sampleEndMs = sampleEndMs
        )
    }

    private fun buildSampleDocument(
        location: Location,
        radioData: Map<String, Any>,
        sampleStartMs: Long,
        sampleEndMs: Long
    ): MutableMap<String, Any> {
        val document = mutableMapOf<String, Any>(
            "timestampStartMs" to sampleStartMs,
            "timestampEndMs" to sampleEndMs,
            "timestampMs" to sampleEndMs,
            "source" to "foregroundService_5s_single_sample",
            "samplingIntervalMs" to CAPTURE_INTERVAL_MS,

            "rawSamplesCount" to 1,
            "validSamplesCount" to 1,
            "tooOldSamplesCount" to 0,
            "repeatedSamplesCount" to if (radioData["isRepeatedRadioSample"] as? Boolean == true) 1 else 0,
            "repeatedRatio" to if (radioData["isRepeatedRadioSample"] as? Boolean == true) 1.0 else 0.0,

            "lat" to location.latitude,
            "lon" to location.longitude,
            "accuracyM" to location.accuracy,
            "speedMps" to location.speed
        )

        document.putAll(radioData)

        putSingleMetricStats(document, "rsrp", radioData["rsrpDbm"])
        putSingleMetricStats(document, "rsrq", radioData["rsrqDb"])
        putSingleMetricStats(document, "rssi", radioData["rssiDbm"])
        putSingleMetricStats(document, "sinr", radioData["sinrDb"])
        putSingleMetricStats(document, "cellInfoAgeMs", radioData["cellInfoAgeMs"])

        document["rsrpAvailabilityPct"] = availabilityPct(radioData["rsrpDbm"])
        document["rsrqAvailabilityPct"] = availabilityPct(radioData["rsrqDb"])
        document["rssiAvailabilityPct"] = availabilityPct(radioData["rssiDbm"])
        document["sinrAvailabilityPct"] = availabilityPct(radioData["sinrDb"])

        document["lastRadioRepeatedCount"] = radioData["radioRepeatedCount"] ?: 0
        radioData["radioSignature"]?.let { document["lastRadioSignature"] = it }

        return document
    }

    private fun putSingleMetricStats(
        document: MutableMap<String, Any>,
        prefix: String,
        value: Any?
    ) {
        val numericValue = (value as? Number)?.toDouble()

        document["${prefix}ValidCount"] = if (numericValue != null) 1 else 0

        if (numericValue == null) return

        document["${prefix}Avg"] = numericValue
        document["${prefix}Median"] = numericValue
        document["${prefix}Min"] = numericValue
        document["${prefix}Max"] = numericValue
        document["${prefix}Std"] = 0.0
    }

    private fun availabilityPct(value: Any?): Double {
        return if (value is Number) 100.0 else 0.0
    }

    private fun sleepRemainingInterval(startedAt: Long) {
        val elapsed = System.currentTimeMillis() - startedAt
        val remaining = CAPTURE_INTERVAL_MS - elapsed

        if (remaining > 0) {
            Thread.sleep(remaining)
        }
    }

    private fun sleepCaptureIntervalQuietly() {
        try {
            Thread.sleep(CAPTURE_INTERVAL_MS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun uploadToFirestore(document: Map<String, Any>) {
        if (!hasInternetConnection()) {
            Log.w(TAG, "No internet - Firestore upload deferred by local CSV retention")
            return
        }

        firestore.collection(FIRESTORE_COLLECTION)
            .add(document)
            .addOnSuccessListener {
                Log.d(
                    TAG,
                    "Firestore write OK | " +
                            "rsrp=${document["rsrpDbm"]} " +
                            "rsrq=${document["rsrqDb"]} " +
                            "rssi=${document["rssiDbm"]} " +
                            "sinr=${document["sinrDb"]}"
                )
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Firestore write failed: ${e.message}", e)
            }
    }

    private fun hasInternetConnection(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        val hasTransport =
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)

        val hasInternet =
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        return hasTransport && hasInternet
    }

    @SuppressLint("MissingPermission")
    private fun getFreshCellInfo(): List<CellInfo>? {
        if (!hasPhonePermission()) {
            Log.w(TAG, "READ_PHONE_STATE not granted")
            return null
        }

        return try {
            var result: List<CellInfo>? = null
            val latch = CountDownLatch(1)

            telephonyManager.requestCellInfoUpdate(
                mainExecutor,
                object : TelephonyManager.CellInfoCallback() {
                    override fun onCellInfo(cellInfo: MutableList<CellInfo>) {
                        result = cellInfo
                        latch.countDown()
                    }

                    override fun onError(errorCode: Int, detail: Throwable?) {
                        Log.w(TAG, "CellInfo callback error=$errorCode: ${detail?.message}")
                        latch.countDown()
                    }
                }
            )

            latch.await(CELLINFO_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            result ?: telephonyManager.allCellInfo
        } catch (e: Exception) {
            Log.e(TAG, "getCellInfo failed: ${e.message}", e)
            runCatching { telephonyManager.allCellInfo }.getOrNull()
        }
    }

    private fun buildRadioData(cellInfoList: List<CellInfo>?): MutableMap<String, Any>? {
        if (cellInfoList.isNullOrEmpty()) return null

        val registeredCell = cellInfoList.firstOrNull { it.isRegistered } ?: return null
        val neighborCells = cellInfoList.filter { !it.isRegistered }

        val ageMs = RadioKpiUtils.cellInfoAgeMs(registeredCell)
        val tooOld = RadioKpiUtils.isCellInfoTooOld(ageMs, MAX_CELLINFO_AGE_MS)

        val data = mutableMapOf<String, Any>(
            "cellInfoAgeMs" to (ageMs ?: -1L),
            "cellInfoTooOld" to tooOld,
            "rsrpAvailable" to false,
            "rsrqAvailable" to false,
            "rssiAvailable" to false,
            "sinrAvailable" to false
        )

        var rsrp: Int? = null
        var rsrq: Int? = null
        var rssi: Int? = null
        var sinr: Double? = null

        when (registeredCell) {
            is CellInfoLte -> {
                val strength = registeredCell.cellSignalStrength
                val identity = registeredCell.cellIdentity

                rsrp = RadioKpiUtils.validRsrp(strength.rsrp)
                rsrq = RadioKpiUtils.validRsrq(strength.rsrq)
                rssi = RadioKpiUtils.lteRssiDbmOrNull(strength)
                sinr = RadioKpiUtils.lteSinrDbOrNull(strength)

                data["networkType"] = "4G LTE"

                identity.ci.takeIfValid()?.let { data["cellId"] = it }
                identity.tac.takeIfValid()?.let { data["tac"] = it }
                identity.pci.takeIfValid()?.let { data["pci"] = it }

                identity.earfcn.takeIfValid()?.let {
                    data["earfcn"] = it
                    data["band"] = getLteBand(it)
                }

                rsrp?.let {
                    data["rsrpDbm"] = it
                    data["rsrpAvailable"] = true
                }

                rsrq?.let {
                    data["rsrqDb"] = it
                    data["rsrqAvailable"] = true
                }

                rssi?.let {
                    data["rssiDbm"] = it
                    data["rssiAvailable"] = true
                }

                sinr?.let {
                    data["sinrDb"] = it
                    data["sinrAvailable"] = true
                }
            }

            is CellInfoNr -> {
                val strength = registeredCell.cellSignalStrength as CellSignalStrengthNr
                val identity = registeredCell.cellIdentity as CellIdentityNr

                rsrp = RadioKpiUtils.validRsrp(strength.ssRsrp)
                rsrq = RadioKpiUtils.validRsrq(strength.ssRsrq)
                sinr = RadioKpiUtils.nrSinrDbOrNull(strength)

                data["networkType"] = "5G NR"

                identity.pci.takeIfValid()?.let { data["pci"] = it }

                if (identity.nci != Long.MAX_VALUE) {
                    data["nci"] = identity.nci
                }

                rsrp?.let {
                    data["rsrpDbm"] = it
                    data["rsrpAvailable"] = true
                }

                rsrq?.let {
                    data["rsrqDb"] = it
                    data["rsrqAvailable"] = true
                }

                sinr?.let {
                    data["sinrDb"] = it
                    data["sinrAvailable"] = true
                }
            }

            else -> return null
        }

        data["hasAnyRadioKpi"] = RadioKpiUtils.hasAnyMainKpi(rsrp, rsrq, rssi, sinr)

        val signature = RadioKpiUtils.buildRadioSignature(
            networkType = data["networkType"]?.toString(),
            pci = data["pci"],
            earfcn = data["earfcn"],
            nci = data["nci"],
            cellId = data["cellId"],
            rsrp = data["rsrpDbm"],
            rsrq = data["rsrqDb"],
            rssi = data["rssiDbm"],
            sinr = data["sinrDb"]
        )

        repeatedCount = if (signature == lastRadioSignature) repeatedCount + 1 else 0
        lastRadioSignature = signature

        data["radioSignature"] = signature
        data["radioRepeatedCount"] = repeatedCount
        data["isRepeatedRadioSample"] = repeatedCount >= 3

        getDownlinkBandwidthKhzOrNull()?.let { downlinkBandwidthKhz ->
            data["dlBandwidthKhz"] = downlinkBandwidthKhz
            data["dlBandwidthMhz"] = downlinkBandwidthKhz / 1_000.0
        }

        data["neighbors"] = buildNeighborList(neighborCells)

        addSystemData(data)

        return data
    }

    private fun buildNeighborList(neighborCells: List<CellInfo>): List<Map<String, Any>> =
        neighborCells.mapNotNull { cell ->
            when (cell) {
                is CellInfoLte -> {
                    val rsrp = RadioKpiUtils.validRsrp(cell.cellSignalStrength.rsrp)
                        ?: return@mapNotNull null

                    buildMap {
                        put("type", "LTE")
                        put("rsrpDbm", rsrp)
                        cell.cellIdentity.earfcn.takeIfValid()?.let { put("earfcn", it) }
                        cell.cellIdentity.pci.takeIfValid()?.let { put("pci", it) }
                    }
                }

                is CellInfoNr -> {
                    val strength = cell.cellSignalStrength as CellSignalStrengthNr
                    val identity = cell.cellIdentity as CellIdentityNr

                    val rsrp = RadioKpiUtils.validRsrp(strength.ssRsrp)
                        ?: return@mapNotNull null

                    buildMap {
                        put("type", "NR")
                        put("rsrpDbm", rsrp)
                        identity.pci.takeIfValid()?.let { put("pci", it) }
                    }
                }

                else -> null
            }
        }

    @Suppress("DEPRECATION")
    private fun addSystemData(data: MutableMap<String, Any>) {
        val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
        val batteryPct = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

        val batteryCharging = batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING

        val memInfo = ActivityManager.MemoryInfo()
        (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memInfo)

        val ramUsedPct = ((memInfo.totalMem - memInfo.availMem).toDouble()
                / memInfo.totalMem * 100).toInt()

        val now = System.currentTimeMillis()
        val deltaTime = (now - lastTrafficTime) / 1_000.0

        val rxBytes = TrafficStats.getTotalRxBytes()
        val txBytes = TrafficStats.getTotalTxBytes()

        val rxKbps = if (deltaTime > 0 && lastRxBytes > 0) {
            (rxBytes - lastRxBytes) * 8 / 1_024.0 / deltaTime
        } else {
            0.0
        }

        val txKbps = if (deltaTime > 0 && lastTxBytes > 0) {
            (txBytes - lastTxBytes) * 8 / 1_024.0 / deltaTime
        } else {
            0.0
        }

        lastRxBytes = rxBytes
        lastTxBytes = txBytes
        lastTrafficTime = now

        val user = auth.currentUser

        data += mapOf(
            "operatorMccMnc" to telephonyManager.simOperator,
            "networkOperatorName" to telephonyManager.networkOperatorName,

            "androidApiLevel" to Build.VERSION.SDK_INT,
            "deviceBrand" to Build.BRAND,
            "deviceManufacturer" to Build.MANUFACTURER,
            "deviceModel" to Build.MODEL,
            "deviceHardware" to Build.HARDWARE,
            "deviceProduct" to Build.PRODUCT,

            "batteryPct" to batteryPct,
            "batteryCharging" to batteryCharging,
            "ramUsedPct" to ramUsedPct,

            "rxKbps" to rxKbps,
            "txKbps" to txKbps,
            "dlMbps" to rxKbps / 1_024.0,
            "ulMbps" to txKbps / 1_024.0,

            "userUid" to (user?.uid ?: "anon"),
            "userShortId" to (user?.uid?.takeLast(6) ?: "anon"),
            "userEmail" to (user?.email ?: "anon"),
            "userName" to (user?.displayName ?: "anon")
        )
    }

    @SuppressLint("MissingPermission")
    private fun getDownlinkBandwidthKhzOrNull(): Int? {
        return try {
            val configs = telephonyManager.javaClass.methods
                .firstOrNull {
                    it.name == "getPhysicalChannelConfigList" && it.parameterTypes.isEmpty()
                }
                ?.invoke(telephonyManager) as? List<*>
                ?: return null

            val primary = configs.firstOrNull() ?: return null

            val bandwidth = listOf(
                "getCellBandwidthDownlinkKhz",
                "getCellBandwidthDownlinkKHz",
                "getBandwidthDownlinkKhz",
                "getBandwidthDownlinkKHz"
            ).firstNotNullOfOrNull { name ->
                primary.javaClass.methods
                    .firstOrNull { it.name == name && it.parameterTypes.isEmpty() }
                    ?.invoke(primary) as? Int
            } ?: return null

            bandwidth.takeIf { it > 0 && it != Int.MAX_VALUE }
        } catch (_: Throwable) {
            null
        }
    }

    private fun getLteBand(earfcn: Int): String = when (earfcn) {
        in 0..599 -> "Band 1 (2100 MHz)"
        in 600..1199 -> "Band 2 (1900 MHz)"
        in 1200..1949 -> "Band 3 (1800 MHz)"
        in 1950..2399 -> "Band 4 (AWS-1 1700/2100 MHz)"
        in 2400..2649 -> "Band 5 (850 MHz)"
        in 2750..3449 -> "Band 7 (2600 MHz)"
        in 3450..3799 -> "Band 8 (900 MHz)"
        in 6150..6449 -> "Band 20 (800 MHz)"
        in 9210..9659 -> "Band 28 (700 MHz)"
        in 66436..67335 -> "Band 66 (AWS-3)"
        else -> "Unknown"
    }

    private fun acquireWakeLock() {
        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Altair::CloudMeasurementLock")
            .apply {
                setReferenceCounted(false)
                acquire(10 * 60 * 1_000L)
            }

        Log.d(TAG, "WakeLock acquired")
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }

        wakeLock = null
        Log.d(TAG, "WakeLock released")
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasPhonePermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun Int.takeIfValid(): Int? {
        return takeIf { it != CellInfo.UNAVAILABLE && it != Int.MAX_VALUE }
    }
}
