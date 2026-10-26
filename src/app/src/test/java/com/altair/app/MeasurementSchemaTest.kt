package com.altair.app

import org.junit.Assert.*
import org.junit.Test

class MeasurementSchemaTest {
    @Test
    fun lteAndNrUseTheSameFieldsWithoutInventingMeasurements() {
        val lte = MeasurementSchema.normalize(mapOf(
            "networkType" to "4G LTE", "cellId" to 123L, "earfcn" to 9285,
            "rsrpDbm" to -88, "batteryCharging" to false, "radioRepeatedCount" to 0
        ))
        val nr = MeasurementSchema.normalize(mapOf(
            "networkType" to "5G NR", "nci" to 68719476734L, "nrarfcn" to 640000
        ))
        assertEquals(90, lte.size)
        assertEquals(90, MeasurementSchema.fields.distinct().size)
        assertEquals(lte.keys, nr.keys)
        assertEquals(-88, lte["rsrpDbm"])
        assertEquals(false, lte["batteryCharging"])
        assertEquals(0, lte["radioRepeatedCount"])
        assertNull(lte["nrarfcn"])
        assertNull(lte["sinrDb"])
        assertNull(nr["earfcn"])
        assertEquals(68719476734L, nr["nci"])
    }

    @Test
    fun normalizationPreservesNeighborDataAndIsIdempotent() {
        val neighbors = listOf(mapOf("type" to "NR", "pci" to 10, "nrarfcn" to 640000))
        val document = MeasurementSchema.normalize(mapOf("neighbors" to neighbors))
        assertEquals(neighbors, document["neighbors"])
        assertEquals(document, MeasurementSchema.normalize(document))
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownFieldsAreRejectedRatherThanSilentlyLost() {
        MeasurementSchema.normalize(mapOf("unexpected" to 1))
    }
}
