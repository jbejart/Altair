package com.altair.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

data class LocalPoint(
    val timestampMs: Long,
    val lat: Double,
    val lon: Double,

    val rsrpDbm: Int? = null,
    val rsrqDb: Int? = null,
    val rssiDbm: Int? = null,
    val tech: String? = null,

    val networkType: String? = null,
    val sinrDb: Double? = null,

    val cellInfoAgeMs: Long? = null,
    val cellInfoTooOld: Boolean = false,

    val isRepeatedRadioSample: Boolean = false,
    val radioRepeatedCount: Int = 0,

    val rsrpAvailable: Boolean = rsrpDbm != null,
    val rsrqAvailable: Boolean = rsrqDb != null,
    val rssiAvailable: Boolean = rssiDbm != null,
    val sinrAvailable: Boolean = sinrDb != null
)

object LocalTrackStore {

    private const val FILE_NAME = "track_local.csv"

    private val HEADER = listOf(
        "timestampStartMs",
        "timestampEndMs",
        "timestampMs",
        "source",

        "rawSamplesCount",
        "validSamplesCount",
        "tooOldSamplesCount",
        "repeatedSamplesCount",

        "lat",
        "lon",
        "accuracyM",
        "speedMps",
        "repeatedRatio",

        "networkType",
        "operatorMccMnc",
        "networkOperatorName",

        "cellId",
        "tac",
        "pci",
        "earfcn",
        "band",
        "nci",

        "rsrpValidCount",
        "rsrpAvg",
        "rsrpMedian",
        "rsrpMin",
        "rsrpMax",
        "rsrpStd",

        "rsrqValidCount",
        "rsrqAvg",
        "rsrqMedian",
        "rsrqMin",
        "rsrqMax",
        "rsrqStd",

        "rssiValidCount",
        "rssiAvg",
        "rssiMedian",
        "rssiMin",
        "rssiMax",
        "rssiStd",

        "sinrValidCount",
        "sinrAvg",
        "sinrMedian",
        "sinrMin",
        "sinrMax",
        "sinrStd",

        "cellInfoAgeMsValidCount",
        "cellInfoAgeMsAvg",
        "cellInfoAgeMsMedian",
        "cellInfoAgeMsMin",
        "cellInfoAgeMsMax",
        "cellInfoAgeMsStd",

        "rsrpAvailabilityPct",
        "rsrqAvailabilityPct",
        "rssiAvailabilityPct",
        "sinrAvailabilityPct",

        "lastRadioRepeatedCount",
        "lastRadioSignature",

        "rxKbps",
        "txKbps",
        "dlMbps",
        "ulMbps",

        "dlBandwidthKhz",
        "dlBandwidthMhz",

        "neighbors",

        "androidApiLevel",
        "deviceBrand",
        "deviceManufacturer",
        "deviceModel",
        "deviceHardware",
        "deviceProduct",

        "batteryPct",
        "batteryCharging",
        "ramUsedPct",

        "userUid",
        "userShortId",
        "userEmail",
        "userName"
    )

    fun appendDocument(context: Context, document: Map<String, Any>) {
        val file = File(context.filesDir, FILE_NAME)
        val isNewFile = !file.exists()

        BufferedWriter(FileWriter(file, true)).use { writer ->
            if (isNewFile) {
                writer.write(HEADER.joinToString(","))
                writer.newLine()
            }

            val row = HEADER.joinToString(",") { key ->
                valueToCsv(document[key])
            }

            writer.write(row)
            writer.newLine()
        }
    }

    fun readAll(context: Context): List<LocalPoint> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()

        val result = mutableListOf<LocalPoint>()

        file.useLines { lines ->
            lines.drop(1).forEach { line ->
                try {
                    val parts = parseCsvLine(line)
                    if (parts.size < HEADER.size) return@forEach

                    fun valueOf(key: String): String? {
                        val index = HEADER.indexOf(key)
                        if (index < 0) return null
                        return parts.getOrNull(index)?.takeIf { it.isNotBlank() }
                    }

                    val timestamp = valueOf("timestampMs")?.toLongOrNull() ?: return@forEach
                    val lat = valueOf("lat")?.toDoubleOrNull() ?: return@forEach
                    val lon = valueOf("lon")?.toDoubleOrNull() ?: return@forEach

                    val networkType = valueOf("networkType")
                    val tech = when {
                        networkType?.contains("NR", ignoreCase = true) == true -> "5G"
                        networkType?.contains("LTE", ignoreCase = true) == true -> "4G"
                        else -> networkType
                    }

                    val rsrp = valueOf("rsrpMedian")?.toDoubleOrNull()?.toInt()
                        ?: valueOf("rsrpAvg")?.toDoubleOrNull()?.toInt()

                    val rsrq = valueOf("rsrqMedian")?.toDoubleOrNull()?.toInt()
                        ?: valueOf("rsrqAvg")?.toDoubleOrNull()?.toInt()

                    val rssi = valueOf("rssiMedian")?.toDoubleOrNull()?.toInt()
                        ?: valueOf("rssiAvg")?.toDoubleOrNull()?.toInt()

                    val sinr = valueOf("sinrMedian")?.toDoubleOrNull()
                        ?: valueOf("sinrAvg")?.toDoubleOrNull()

                    val repeatedCount = valueOf("lastRadioRepeatedCount")?.toIntOrNull() ?: 0
                    val cellInfoAgeMs = valueOf("cellInfoAgeMsMedian")?.toDoubleOrNull()?.toLong()
                        ?: valueOf("cellInfoAgeMsAvg")?.toDoubleOrNull()?.toLong()

                    result.add(
                        LocalPoint(
                            timestampMs = timestamp,
                            lat = lat,
                            lon = lon,
                            rsrpDbm = rsrp,
                            rsrqDb = rsrq,
                            rssiDbm = rssi,
                            tech = tech,
                            networkType = networkType,
                            sinrDb = sinr,
                            cellInfoAgeMs = cellInfoAgeMs,
                            cellInfoTooOld = false,
                            isRepeatedRadioSample = repeatedCount >= 3,
                            radioRepeatedCount = repeatedCount,
                            rsrpAvailable = rsrp != null,
                            rsrqAvailable = rsrq != null,
                            rssiAvailable = rssi != null,
                            sinrAvailable = sinr != null
                        )
                    )
                } catch (_: Exception) {
                    // Ignore corrupted rows
                }
            }
        }

        return result
    }

    fun clear(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        if (file.exists()) file.delete()
    }

    fun getFile(context: Context): File {
        return File(context.filesDir, FILE_NAME)
    }

    private fun valueToCsv(value: Any?): String {
        val text = when (value) {
            null -> ""
            is Map<*, *> -> JSONObject(value).toString()
            is Iterable<*> -> JSONArray(value.toList()).toString()
            is Array<*> -> JSONArray(value.toList()).toString()
            else -> value.toString()
        }

        return escapeCsv(text)
    }

    private fun escapeCsv(value: String): String {
        val mustQuote = value.contains(",") ||
                value.contains("\"") ||
                value.contains("\n") ||
                value.contains("\r")

        if (!mustQuote) return value

        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var insideQuotes = false
        var i = 0

        while (i < line.length) {
            val char = line[i]

            when {
                char == '"' && insideQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }

                char == '"' -> {
                    insideQuotes = !insideQuotes
                }

                char == ',' && !insideQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }

                else -> current.append(char)
            }

            i++
        }

        result.add(current.toString())
        return result
    }
}