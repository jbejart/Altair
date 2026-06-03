package com.altair.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.math.floor

class MapFragment : Fragment(R.layout.fragment_map) {

    private lateinit var map: MapView

    private val localMarkers = mutableListOf<Marker>()
    private val coveragePolygons = mutableListOf<Polygon>()

    private var myLocationOverlay: MyLocationNewOverlay? = null

    private lateinit var fabToggleFilters: FloatingActionButton
    private lateinit var filtersCard: MaterialCardView
    private lateinit var signalCard: MaterialCardView
    private lateinit var txtCurrentSignal: TextView

    private lateinit var mapTechnologyChipGroup: ChipGroup
    private lateinit var operatorChipGroup: ChipGroup

    private lateinit var measurementSwitch: SwitchMaterial

    private var selectedOperator: String = DEFAULT_OPERATOR
    private var selectedTechnology: String = DEFAULT_TECHNOLOGY

    private fun prefs() =
        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureOsmdroid()
        setupMap(view)
        setupUi(view)
        setupFilterListeners()
        setupMeasurementSwitch()

        requestBasePermissions()
        enableLocationOverlay()

        loadLocalTrackOnMap()
        updateSignalCardFromLocal()

        setupMapMoveListener()
        refreshCoverageOverlay()
    }

    override fun onResume() {
        super.onResume()

        map.onResume()

        syncSwitchFromRunningState()
        enableLocationOverlay()
        loadLocalTrackOnMap()
        updateSignalCardFromLocal()
        refreshCoverageOverlay()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    // ===================== INITIAL CONFIGURATION =====================

    private fun configureOsmdroid() {
        val context = requireContext()

        Configuration.getInstance().load(
            context,
            context.getSharedPreferences(OSMDROID_PREFS_NAME, Context.MODE_PRIVATE)
        )

        Configuration.getInstance().userAgentValue = context.packageName
    }

    private fun setupMap(view: View) {
        map = view.findViewById(R.id.map)

        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(DEFAULT_ZOOM)
        map.controller.setCenter(GeoPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE))
    }

    private fun setupUi(view: View) {
        fabToggleFilters = view.findViewById(R.id.fabToggleFilters)
        filtersCard = view.findViewById(R.id.cardFilters)
        signalCard = view.findViewById(R.id.cardSignal)
        txtCurrentSignal = view.findViewById(R.id.txtSignalNow)

        mapTechnologyChipGroup = view.findViewById(R.id.chipTechGroupMap)
        operatorChipGroup = view.findViewById(R.id.chipOpGroup)

        measurementSwitch = view.findViewById(R.id.switchMedicionMap)

        fabToggleFilters.setOnClickListener {
            filtersCard.isVisible = !filtersCard.isVisible
        }

        view.findViewById<View>(R.id.chip4gMap)?.performClick()
        view.findViewById<View>(R.id.chipMovistar)?.performClick()
    }

    private fun setupFilterListeners() {
        mapTechnologyChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull()

            selectedTechnology = when (checkedId) {
                R.id.chip2gMap -> "2G"
                R.id.chip3gMap -> "3G"
                R.id.chip4gMap -> "4G"
                R.id.chip5gMap -> "5G"
                else -> DEFAULT_TECHNOLOGY
            }

            refreshCoverageOverlay()
        }

        operatorChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull()

            selectedOperator = when (checkedId) {
                R.id.chipMovistar -> "Movistar"
                R.id.chipClaro -> "Claro"
                R.id.chipEntel -> "Entel"
                R.id.chipBitel -> "Bitel"
                else -> DEFAULT_OPERATOR
            }

            refreshCoverageOverlay()
        }
    }

    private fun setupMeasurementSwitch() {
        syncSwitchFromRunningState()

        measurementSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startBothMeasurementsFromMapSwitch()
            } else {
                stopBothMeasurementsFromMapSwitch()
            }
        }
    }

    private fun setupMapMoveListener() {
        map.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                refreshCoverageOverlay()
                return true
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                refreshCoverageOverlay()
                return true
            }
        })
    }

    // ===================== SWITCH: LOCAL + FIREBASE =====================

    private fun syncSwitchFromRunningState() {
        val context = requireContext()

        val isAnyServiceRunning =
            ServiceStateStore.isLocalRunning(context) ||
                    ServiceStateStore.isFirebaseRunning(context)

        prefs().edit {
            putBoolean(KEY_MEASUREMENT_ACTIVE, isAnyServiceRunning)
        }

        setSwitchCheckedWithoutCallback(isAnyServiceRunning)
    }

    private fun setSwitchCheckedWithoutCallback(checked: Boolean) {
        measurementSwitch.setOnCheckedChangeListener(null)
        measurementSwitch.isChecked = checked

        measurementSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startBothMeasurementsFromMapSwitch()
            } else {
                stopBothMeasurementsFromMapSwitch()
            }
        }
    }

    private fun startBothMeasurementsFromMapSwitch() {
        val context = requireContext()

        if (!hasBasePermissions()) {
            requestBasePermissions()

            Toast.makeText(
                context,
                "Grant location and phone permissions to start measurement",
                Toast.LENGTH_LONG
            ).show()

            disableMeasurementSwitch()
            return
        }

        if (!hasBackgroundLocationPermission()) {
            requestBackgroundLocationPermission()

            Toast.makeText(
                context,
                "Enable location as 'Allow all the time' to measure in the background",
                Toast.LENGTH_LONG
            ).show()

            disableMeasurementSwitch()
            return
        }

        ContextCompat.startForegroundService(
            context,
            Intent(context, LocalMeasurementService::class.java)
        )
        ServiceStateStore.setLocalRunning(context, true)

        ContextCompat.startForegroundService(
            context,
            Intent(context, ForegroundMeasurementService::class.java)
        )
        ServiceStateStore.setFirebaseRunning(context, true)

        prefs().edit {
            putBoolean(KEY_MEASUREMENT_ACTIVE, true)
        }

        setSwitchCheckedWithoutCallback(true)

        Toast.makeText(
            context,
            "✅ Measurement started (Local + Firebase)",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun stopBothMeasurementsFromMapSwitch() {
        val context = requireContext()

        context.stopService(Intent(context, LocalMeasurementService::class.java))
        ServiceStateStore.setLocalRunning(context, false)

        context.stopService(Intent(context, ForegroundMeasurementService::class.java))
        ServiceStateStore.setFirebaseRunning(context, false)

        prefs().edit {
            putBoolean(KEY_MEASUREMENT_ACTIVE, false)
        }

        setSwitchCheckedWithoutCallback(false)

        Toast.makeText(
            context,
            "⏹ Measurement stopped",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun disableMeasurementSwitch() {
        prefs().edit {
            putBoolean(KEY_MEASUREMENT_ACTIVE, false)
        }

        setSwitchCheckedWithoutCallback(false)
    }

    // ===================== CURRENT SIGNAL CARD =====================

    private fun updateSignalCardFromLocal() {
        val points = LocalTrackStore.readAll(requireContext())

        if (points.isEmpty()) {
            txtCurrentSignal.text = "Operator: —   Tech: —   Signal: —"
            return
        }

        val lastPoint = points.last()
        val signal = lastPoint.rsrpDbm ?: lastPoint.rssiDbm

        val signalText = signal?.let { "$it dBm" } ?: "—"
        val technologyText = lastPoint.tech ?: "—"

        txtCurrentSignal.text = "Tech: $technologyText   Signal: $signalText"
    }

    // ===================== OSIPTEL COVERAGE =====================

    data class CoveragePoint(
        val lat: Double,
        val lon: Double
    )

    private fun refreshCoverageOverlay() {
        coveragePolygons.forEach { polygon ->
            map.overlays.remove(polygon)
        }
        coveragePolygons.clear()

        val boundingBox = map.boundingBox

        val coveragePoints = queryCoveragePoints(
            operator = selectedOperator,
            technology = selectedTechnology,
            minLat = boundingBox.latSouth,
            maxLat = boundingBox.latNorth,
            minLon = boundingBox.lonWest,
            maxLon = boundingBox.lonEast
        )

        if (coveragePoints.isEmpty()) {
            map.invalidate()
            return
        }

        val cellSize = getCoverageCellSize(map.zoomLevelDouble)
        val grid = buildCoverageGrid(coveragePoints, cellSize)
        val fillColor = getCoverageFillColor(selectedTechnology)

        for (cell in grid.keys) {
            val polygon = createCoveragePolygon(
                cell = cell,
                cellSize = cellSize,
                fillColor = fillColor
            )

            map.overlays.add(polygon)
            coveragePolygons.add(polygon)
        }

        map.invalidate()
    }

    private fun getCoverageCellSize(zoom: Double): Double {
        return when {
            zoom >= 16.0 -> 0.0012
            zoom >= 14.0 -> 0.0020
            else -> 0.0035
        }
    }

    private data class CellKey(
        val x: Int,
        val y: Int
    )

    private fun buildCoverageGrid(
        points: List<CoveragePoint>,
        cellSize: Double
    ): Map<CellKey, Boolean> {
        val grid = HashMap<CellKey, Boolean>()

        for (point in points) {
            val x = floor(point.lat / cellSize).toInt()
            val y = floor(point.lon / cellSize).toInt()

            grid[CellKey(x, y)] = true
        }

        return grid
    }

    private fun getCoverageFillColor(technology: String): Int {
        return when (technology) {
            "2G" -> Color.argb(85, 63, 81, 181)
            "3G" -> Color.argb(85, 76, 175, 80)
            "4G" -> Color.argb(85, 255, 152, 0)
            "5G" -> Color.argb(85, 233, 30, 99)
            else -> Color.argb(85, 63, 81, 181)
        }
    }

    @Suppress("DEPRECATION")
    private fun createCoveragePolygon(
        cell: CellKey,
        cellSize: Double,
        fillColor: Int
    ): Polygon {
        val lat0 = cell.x * cellSize
        val lon0 = cell.y * cellSize
        val lat1 = lat0 + cellSize
        val lon1 = lon0 + cellSize

        return Polygon(map).apply {
            setPoints(
                listOf(
                    GeoPoint(lat0, lon0),
                    GeoPoint(lat0, lon1),
                    GeoPoint(lat1, lon1),
                    GeoPoint(lat1, lon0)
                )
            )

            this.fillColor = fillColor
            this.strokeColor = Color.TRANSPARENT
        }
    }

    private fun queryCoveragePoints(
        operator: String,
        technology: String,
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): List<CoveragePoint> {
        val databaseFile = OsiptelDb.ensureDb(requireContext())
        val database = SQLiteDatabase.openDatabase(
            databaseFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        )

        val technologyColumn = when (technology) {
            "2G" -> "g2"
            "3G" -> "g3"
            "4G" -> "g4"
            "5G" -> "g5"
            else -> "g4"
        }

        val sql = """
            SELECT lat, lon
            FROM coverage_points
            WHERE op = ?
              AND $technologyColumn = 1
              AND lat BETWEEN ? AND ?
              AND lon BETWEEN ? AND ?
            LIMIT $MAX_COVERAGE_POINTS
        """.trimIndent()

        val points = mutableListOf<CoveragePoint>()

        database.rawQuery(
            sql,
            arrayOf(
                operator,
                minLat.toString(),
                maxLat.toString(),
                minLon.toString(),
                maxLon.toString()
            )
        ).use { cursor ->
            while (cursor.moveToNext()) {
                points.add(
                    CoveragePoint(
                        lat = cursor.getDouble(0),
                        lon = cursor.getDouble(1)
                    )
                )
            }
        }

        database.close()
        return points
    }

    // ===================== PERMISSIONS =====================

    private fun hasBasePermissions(): Boolean {
        val context = requireContext()

        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val phoneStateGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocationGranted && phoneStateGranted
    }

    private fun hasBackgroundLocationPermission(): Boolean {
        val context = requireContext()

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestBasePermissions() {
        val context = requireContext()
        val permissions = mutableListOf<String>()

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions += Manifest.permission.ACCESS_FINE_LOCATION
            permissions += Manifest.permission.ACCESS_COARSE_LOCATION
        }

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions += Manifest.permission.READ_PHONE_STATE
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                permissions.toTypedArray(),
                REQUEST_BASE_PERMISSIONS
            )
        }
    }

    private fun requestBackgroundLocationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
            REQUEST_BACKGROUND_LOCATION
        )
    }

    private fun enableLocationOverlay() {
        val context = requireContext()

        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineLocationGranted) return

        if (myLocationOverlay == null) {
            myLocationOverlay = MyLocationNewOverlay(
                GpsMyLocationProvider(context),
                map
            ).apply {
                enableMyLocation()
                enableFollowLocation()
            }

            map.overlays.add(myLocationOverlay)
        }
    }

    // ===================== LOCAL TRACK =====================

    private fun loadLocalTrackOnMap() {
        localMarkers.forEach { marker ->
            map.overlays.remove(marker)
        }
        localMarkers.clear()

        val points = LocalTrackStore.readAll(requireContext())

        if (points.isEmpty()) {
            map.invalidate()
            return
        }

        for (point in points) {
            val signalValue = point.rsrpDbm ?: point.rssiDbm ?: DEFAULT_LOW_SIGNAL
            val markerColor = getSignalColor(signalValue)

            val marker = Marker(map).apply {
                position = GeoPoint(point.lat, point.lon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = buildMarkerTitle(point.tech, signalValue)
                icon = getDotMarker(markerColor)
            }

            map.overlays.add(marker)
            localMarkers.add(marker)
        }

        map.invalidate()
    }

    private fun buildMarkerTitle(technology: String?, signalValue: Int): String {
        return "${technology.orEmpty()} $signalValue dBm".trim()
    }

    private fun getSignalColor(signalValue: Int): Int {
        return when {
            signalValue >= -75 -> Color.GREEN
            signalValue in -95..-76 -> Color.YELLOW
            signalValue in -100..-96 -> Color.rgb(255, 165, 0)
            else -> Color.RED
        }
    }

    private fun getDotMarker(color: Int): Drawable {
        val density = resources.displayMetrics.density

        val sizePx = (MARKER_SIZE_DP * density).toInt().coerceAtLeast(MIN_MARKER_SIZE_PX)
        val strokePx = MARKER_STROKE_DP * density
        val radius = sizePx / 2f

        val bitmap = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawCircle(radius, radius, radius - strokePx, paint)

        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        paint.strokeWidth = strokePx
        canvas.drawCircle(radius, radius, radius - strokePx / 2f, paint)

        return bitmap.toDrawable(resources)
    }

    companion object {
        private const val OSMDROID_PREFS_NAME = "osmdroid"

        private const val PREFS_NAME = "altair_prefs"
        private const val KEY_MEASUREMENT_ACTIVE = "measurement_active"

        private const val DEFAULT_OPERATOR = "Movistar"
        private const val DEFAULT_TECHNOLOGY = "4G"

        private const val DEFAULT_LATITUDE = -16.3989
        private const val DEFAULT_LONGITUDE = -71.5350
        private const val DEFAULT_ZOOM = 13.5

        private const val DEFAULT_LOW_SIGNAL = -120

        private const val MARKER_SIZE_DP = 10
        private const val MARKER_STROKE_DP = 2
        private const val MIN_MARKER_SIZE_PX = 6

        private const val MAX_COVERAGE_POINTS = 6000

        private const val REQUEST_BASE_PERMISSIONS = 900
        private const val REQUEST_BACKGROUND_LOCATION = 901
    }
}