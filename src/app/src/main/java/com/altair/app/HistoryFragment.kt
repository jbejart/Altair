package com.altair.app

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HistoryFragment : Fragment(R.layout.fragment_history) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val historyRecyclerView = view.findViewById<RecyclerView>(R.id.recyclerHistory)

        historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        historyRecyclerView.setHasFixedSize(true)

        val points = LocalTrackStore.readAll(requireContext())
            .takeLast(200)
            .reversed()

        historyRecyclerView.adapter = HistoryPointsAdapter(points)
    }
}