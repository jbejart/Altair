package com.altair.app

/** Shared top-level fields for local CSV rows and Firestore documents. */
object MeasurementSchema {
    val fields: List<String> = listOf(
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
        "nrarfcn",

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

        "samplingIntervalMs",
        "cellInfoAgeMs",
        "cellInfoTooOld",
        "rsrpAvailable",
        "rsrqAvailable",
        "rssiAvailable",
        "sinrAvailable",
        "rsrpDbm",
        "rsrqDb",
        "rssiDbm",
        "sinrDb",
        "hasAnyRadioKpi",
        "radioSignature",
        "radioRepeatedCount",
        "isRepeatedRadioSample"
    )

    /** Keep missing measurements as null, without substituting a measured zero. */
    fun normalize(document: Map<String, Any?>): Map<String, Any?> {
        require(document.keys.all { it in fields }) {
            "Measurement contains fields missing from MeasurementSchema"
        }
        return fields.associateWith { document[it] }
    }
}
