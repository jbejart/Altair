package com.altair.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.chip.ChipGroup
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class StatsFragment : Fragment(R.layout.fragment_stats) {

    private lateinit var txtKpis: TextView
    private lateinit var txtEmpty: TextView
    private lateinit var pie: PieChart
    private lateinit var line: LineChart

    private lateinit var btnClearLocalStats: Button
    private lateinit var btnOpenHistory: Button
    private lateinit var btnExportCsv: Button

    private lateinit var chipTechGroup: ChipGroup
    private var selectedTech: String = DEFAULT_TECH

    private var pendingCsvFile: File? = null

    private val createCsvDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                pendingCsvFile = null
                return@registerForActivityResult
            }

            val destinationUri = result.data?.data
            val sourceFile = pendingCsvFile

            if (destinationUri == null || sourceFile == null) {
                showToast("Could not select the file destination.")
                pendingCsvFile = null
                return@registerForActivityResult
            }

            copyCsvToSelectedLocation(sourceFile, destinationUri)
            pendingCsvFile = null
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupButtons()
        setupChips(view)
        setupPie()
        setupLine()
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun bindViews(view: View) {
        txtKpis = view.findViewById(R.id.txtKpis)
        txtEmpty = view.findViewById(R.id.txtEmptyStats)
        pie = view.findViewById(R.id.pieTechQuality)
        line = view.findViewById(R.id.lineSignal)

        btnClearLocalStats = view.findViewById(R.id.btnClearLocalStats)
        btnOpenHistory = view.findViewById(R.id.btnOpenHistory)
        btnExportCsv = view.findViewById(R.id.btnExportCsv)

        chipTechGroup = view.findViewById(R.id.chipTechGroup)
    }

    private fun setupButtons() {
        btnClearLocalStats.setOnClickListener {
            clearLocalData()
        }

        btnOpenHistory.setOnClickListener {
            (requireActivity() as? HomeActivity)?.openFragment(HistoryFragment())
        }

        btnExportCsv.setOnClickListener {
            exportLocalCsv()
        }
    }

    private fun setupChips(view: View) {
        chipTechGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull()

            selectedTech = when (checkedId) {
                R.id.chip2g -> "2G"
                R.id.chip3g -> "3G"
                R.id.chip4g -> "4G"
                R.id.chip5g -> "5G"
                else -> DEFAULT_TECH
            }

            render()
        }

        view.findViewById<View>(R.id.chip4g)?.performClick()
    }

    // ---------------- Export CSV ----------------

    private fun exportLocalCsv() {
        val csvFile = findLocalCsvFile()

        if (csvFile == null || !csvFile.exists() || csvFile.length() == 0L) {
            showToast("No local CSV available to export.")
            return
        }

        pendingCsvFile = csvFile

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val suggestedName = "altair_measurements_$timestamp.csv"

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = CSV_MIME_TYPE
            putExtra(Intent.EXTRA_TITLE, suggestedName)
        }

        createCsvDocumentLauncher.launch(intent)
    }

    private fun copyCsvToSelectedLocation(sourceFile: File, destinationUri: Uri) {
        try {
            val resolver = requireContext().contentResolver

            resolver.openOutputStream(destinationUri)?.use { output ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Could not open the destination file.")

            showToast("CSV exported successfully.")
        } catch (e: Exception) {
            showToast("Error exporting CSV: ${e.message ?: "unknown"}")
        }
    }

    private fun findLocalCsvFile(): File? {
        val ctx = requireContext()

        val possibleFiles = listOf(
            File(ctx.filesDir, LOCAL_CSV_NAME),
            File(ctx.getExternalFilesDir(null), LOCAL_CSV_NAME),
            File(ctx.getExternalFilesDir("Altair"), LOCAL_CSV_NAME),
            File(ctx.getExternalFilesDir("Documents"), LOCAL_CSV_NAME)
        )

        return possibleFiles.firstOrNull { it.exists() && it.length() > 0L }
    }

    // ---------------- Clear local data ----------------

    private fun clearLocalData() {
        val ctx = requireContext()

        ctx.stopService(Intent(ctx, LocalMeasurementService::class.java))
        ServiceStateStore.setLocalRunning(ctx, false)

        ctx.stopService(Intent(ctx, ForegroundMeasurementService::class.java))
        ServiceStateStore.setFirebaseRunning(ctx, false)

        LocalTrackStore.clear(ctx)

        showToast("Local data deleted")
        render()
    }

    // ---------------- Render KPIs + Charts ----------------

    private fun render() {
        val points = LocalTrackStore.readAll(requireContext())

        if (points.isEmpty()) {
            showEmptyState()
            return
        }

        txtEmpty.visibility = View.GONE

        val data = points.takeLast(MAX_POINTS_TO_RENDER)

        txtKpis.text = computeKpis(data)
        renderPieTechQuality(data)
        renderLineSignal(data)
    }

    private fun showEmptyState() {
        txtEmpty.visibility = View.VISIBLE
        txtKpis.text = "No data"

        pie.clear()
        line.clear()

        pie.centerText = "$selectedTech\nNo data"

        pie.invalidate()
        line.invalidate()
    }

    // ---------------- KPIs ----------------

    private fun computeKpis(data: List<LocalPoint>): String {
        fun scoreDbm(point: LocalPoint): Int? = point.rsrpDbm ?: point.rssiDbm

        val scores = data.mapNotNull { scoreDbm(it) }
        val total = data.size
        val scored = scores.size

        val avg = scores.takeIf { it.isNotEmpty() }?.average()
        val p95 = percentile95(scores)

        val goodPct = if (scores.isEmpty()) {
            null
        } else {
            val good = scores.count { it >= GOOD_SIGNAL_THRESHOLD_DBM }
            100.0 * good / scores.size
        }

        val techCounts = data.groupingBy { it.tech ?: "?" }.eachCount()
        val dominantTech = techCounts.maxByOrNull { it.value }?.key ?: "?"

        val avgTxt = if (avg == null) {
            "—"
        } else {
            String.format(Locale.US, "%.1f dBm", avg)
        }

        val p95Txt = if (p95 == null) {
            "—"
        } else {
            String.format(Locale.US, "%d dBm", p95)
        }

        val goodTxt = if (goodPct == null) {
            "—"
        } else {
            String.format(Locale.US, "%.0f%%", goodPct)
        }

        return buildString {
            append("Samples: $total (with signal: $scored)\n")
            append("Average: $avgTxt   |   P95: $p95Txt\n")
            append("% Good+Excellent: $goodTxt\n")
            append("Dominant technology: $dominantTech\n")
        }
    }

    private fun percentile95(values: List<Int>): Int? {
        if (values.isEmpty()) return null

        val sorted = values.sorted()
        val idx = ((sorted.size - 1) * PERCENTILE_95).roundToInt()
            .coerceIn(0, sorted.size - 1)

        return sorted[idx]
    }

    // ---------------- PIE ----------------

    private fun setupPie() {
        pie.description.isEnabled = false
        pie.setNoDataText("No data")
        pie.legend.textColor = WHITE
        pie.setUsePercentValues(true)
        pie.setDrawEntryLabels(false)
        pie.isRotationEnabled = false
        pie.isDrawHoleEnabled = true
        pie.holeRadius = 58f
        pie.transparentCircleRadius = 62f
        pie.setCenterTextColor(WHITE)
        pie.centerText = "Quality"
    }

    private fun renderPieTechQuality(data: List<LocalPoint>) {
        fun score(point: LocalPoint): Int? = point.rsrpDbm ?: point.rssiDbm

        fun bucket(scoreDbm: Int): Int = when {
            scoreDbm >= EXCELLENT_SIGNAL_DBM -> 0
            scoreDbm >= GOOD_SIGNAL_THRESHOLD_DBM -> 1
            scoreDbm >= REGULAR_SIGNAL_DBM -> 2
            else -> 3
        }

        val filtered = data.filter { (it.tech ?: "") == selectedTech }

        if (filtered.isEmpty()) {
            pie.clear()
            pie.centerText = "$selectedTech\nNo data"
            pie.invalidate()
            return
        }

        val counts = IntArray(4)

        filtered.forEach { point ->
            val signal = score(point) ?: return@forEach
            counts[bucket(signal)]++
        }

        val labels = arrayOf("Excellent", "Good", "Fair", "Poor")
        val entries = mutableListOf<PieEntry>()

        for (i in counts.indices) {
            if (counts[i] > 0) {
                entries.add(PieEntry(counts[i].toFloat(), labels[i]))
            }
        }

        if (entries.isEmpty()) {
            pie.clear()
            pie.centerText = "$selectedTech\nNo signal"
            pie.invalidate()
            return
        }

        val dataSet = PieDataSet(entries, "").apply {
            setDrawValues(true)
            valueTextColor = WHITE
            valueTextSize = 12f
            colors = listOf(
                GREEN,
                YELLOW,
                ORANGE,
                RED
            )
        }

        pie.data = PieData(dataSet)
        pie.centerText = "$selectedTech\n(${filtered.size} samples)"
        pie.invalidate()
    }

    // ---------------- LINE ----------------

    private fun setupLine() {
        line.description.isEnabled = false
        line.setNoDataText("No data")
        line.axisRight.isEnabled = false
        line.axisLeft.textColor = WHITE
        line.xAxis.textColor = WHITE
        line.legend.textColor = WHITE
        line.xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        line.xAxis.setDrawGridLines(false)
        line.axisLeft.setDrawGridLines(true)

        line.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format(Locale.US, "%.0f min", value)
            }
        }
    }

    private fun renderLineSignal(data: List<LocalPoint>) {
        fun score(point: LocalPoint): Int? = point.rsrpDbm ?: point.rssiDbm

        val start = data.first().timestampMs

        val entries = data.mapNotNull { point ->
            val signal = score(point) ?: return@mapNotNull null
            val minutes = (point.timestampMs - start) / 60000f
            Entry(minutes, signal.toFloat())
        }

        if (entries.isEmpty()) {
            line.clear()
            line.invalidate()
            return
        }

        val dataSet = LineDataSet(entries, "Signal (dBm)").apply {
            setDrawValues(false)
            setDrawCircles(false)
            lineWidth = 2f
            color = BLUE
            mode = LineDataSet.Mode.LINEAR
        }

        line.data = LineData(dataSet)
        line.invalidate()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val DEFAULT_TECH = "4G"
        private const val LOCAL_CSV_NAME = "track_local.csv"
        private const val CSV_MIME_TYPE = "text/csv"
        private const val MAX_POINTS_TO_RENDER = 3000

        private const val PERCENTILE_95 = 0.95

        private const val EXCELLENT_SIGNAL_DBM = -75
        private const val GOOD_SIGNAL_THRESHOLD_DBM = -95
        private const val REGULAR_SIGNAL_DBM = -105

        private val WHITE = "#FFFFFF".toColorInt()
        private val GREEN = "#2ECC71".toColorInt()
        private val YELLOW = "#F1C40F".toColorInt()
        private val ORANGE = "#E67E22".toColorInt()
        private val RED = "#E74C3C".toColorInt()
        private val BLUE = "#5DADE2".toColorInt()
    }
}