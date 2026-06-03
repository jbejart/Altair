package com.altair.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryPointsAdapter(
    private val data: List<LocalPoint>
) : RecyclerView.Adapter<HistoryPointsAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val firstRow: TextView = view.findViewById(R.id.txtFila1)
        val secondRow: TextView = view.findViewById(R.id.txtFila2)
        val chip: TextView? = view.findViewById(R.id.txtChip)
    }

    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medicion, parent, false)

        return VH(view)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val point = data[position]

        val time = formatTime(point.timestampMs)
        val technology = point.tech?.takeIf { it.isNotBlank() } ?: "?"

        val signalText = getSignalText(point)

        holder.firstRow.text = "$time | $signalText"

        holder.secondRow.text = buildString {
            append(formatCoordinates(point.lat, point.lon))

            point.rsrqDb?.let { rsrq ->
                append(" | RSRQ $rsrq dB")
            }
        }

        holder.chip?.text = technology
    }

    private fun formatTime(timestampMs: Long): String {
        return timeFormatter.format(Date(timestampMs))
    }

    private fun formatCoordinates(lat: Double, lon: Double): String {
        return String.format(Locale.US, "%.6f, %.6f", lat, lon)
    }

    private fun getSignalText(point: LocalPoint): String {
        return when {
            point.rsrpDbm != null -> "${point.rsrpDbm} dBm RSRP"
            point.rssiDbm != null -> "${point.rssiDbm} dBm RSSI"
            else -> "No signal"
        }
    }
}