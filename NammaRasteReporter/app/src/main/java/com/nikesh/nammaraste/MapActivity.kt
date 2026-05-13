package com.nikesh.nammaraste

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nikesh.nammaraste.databinding.ActivityMapBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MapActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMapBinding

    private val reportDao by lazy {
        (application as NammaRasteApplication).database.reportDao()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }
        observeReports()
    }

    private fun observeReports() {
        lifecycleScope.launch {
            reportDao.observeReports().collectLatest { reports ->
                val mappedReports = reports.filter { it.latitude != null && it.longitude != null }
                binding.reportMapView.setReports(mappedReports)
                binding.mapSummaryText.text = if (mappedReports.isEmpty()) {
                    getString(R.string.no_location_reports)
                } else {
                    resources.getQuantityString(
                        R.plurals.map_report_count,
                        mappedReports.size,
                        mappedReports.size
                    )
                }
            }
        }
    }
}
