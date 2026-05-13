package com.nikesh.nammaraste

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nikesh.nammaraste.data.InfrastructureReport
import com.nikesh.nammaraste.databinding.ActivityTrackerBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrackerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTrackerBinding
    private var isAdminLoggedIn = false

    private val reportDao by lazy {
        (application as NammaRasteApplication).database.reportDao()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrackerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }
        binding.trackButton.setOnClickListener { trackTicket() }
        setupAdminStatusUpdate()
        setupAdminLogin()
        binding.clearReportsButton.setOnClickListener { confirmClearAllReports() }
        binding.openMapButton.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }
        observeReports()
    }

    private fun setupAdminStatusUpdate() {
        val statusOptions = listOf("Submitted", "In Progress", "Resolved")
        binding.statusUpdateInput.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                statusOptions
            )
        )
        binding.statusUpdateInput.setText(statusOptions.first(), false)
        binding.updateStatusButton.setOnClickListener { updateTicketStatus() }
    }

    private fun setupAdminLogin() {
        updateAdminVisibility()
        binding.adminLoginButton.setOnClickListener {
            val adminId = binding.adminIdInput.text?.toString()?.trim().orEmpty()
            val password = binding.adminPasswordInput.text?.toString().orEmpty()
            if (adminId == ADMIN_ID && password == ADMIN_PASSWORD) {
                isAdminLoggedIn = true
                binding.adminPasswordInput.text?.clear()
                binding.adminLoginResult.text = getString(R.string.admin_logged_in)
                updateAdminVisibility()
            } else {
                binding.adminLoginResult.text = getString(R.string.invalid_admin_login)
            }
        }
        binding.adminLogoutButton.setOnClickListener {
            isAdminLoggedIn = false
            binding.adminTicketInput.text?.clear()
            binding.adminStatusResult.text = getString(R.string.admin_status_hint)
            updateAdminVisibility()
        }
    }

    private fun updateAdminVisibility() {
        binding.adminLoginPanel.visibility = if (isAdminLoggedIn) View.GONE else View.VISIBLE
        binding.adminControlsPanel.visibility = if (isAdminLoggedIn) View.VISIBLE else View.GONE
    }

    private fun observeReports() {
        lifecycleScope.launch {
            reportDao.observeReports().collectLatest { reports ->
                binding.reportCountText.text = resources.getQuantityString(
                    R.plurals.report_count,
                    reports.size,
                    reports.size
                )
                binding.latestTicketText.text = reports.firstOrNull()?.ticketId
                    ?: getString(R.string.no_reports_yet)
                renderAllReports(reports)
            }
        }
    }

    private fun trackTicket() {
        val ticketId = binding.ticketInput.text?.toString()?.trim().orEmpty()
        if (ticketId.isBlank()) {
            binding.ticketInput.error = getString(R.string.enter_ticket_id)
            return
        }

        lifecycleScope.launch {
            val report = withContext(Dispatchers.IO) { reportDao.findByTicketId(ticketId) }
            binding.ticketResult.text = if (report == null) {
                getString(R.string.ticket_not_found)
            } else {
                getString(
                    R.string.ticket_result,
                    report.ticketId,
                    report.issueType,
                    report.severity,
                    report.description.ifBlank { getString(R.string.no_description) },
                    report.status,
                    formatDate(report.createdAtMillis)
                )
            }
        }
    }

    private fun updateTicketStatus() {
        if (!isAdminLoggedIn) {
            binding.adminLoginResult.text = getString(R.string.admin_login_required)
            return
        }
        val ticketId = binding.adminTicketInput.text?.toString()?.trim().orEmpty()
        val status = binding.statusUpdateInput.text?.toString()?.trim().orEmpty()
        if (ticketId.isBlank()) {
            binding.adminTicketInput.error = getString(R.string.enter_ticket_id)
            return
        }
        if (status.isBlank()) {
            binding.statusUpdateInput.error = getString(R.string.choose_status)
            return
        }

        lifecycleScope.launch {
            val updatedRows = withContext(Dispatchers.IO) {
                reportDao.updateStatus(ticketId, status)
            }
            binding.adminStatusResult.text = if (updatedRows == 0) {
                getString(R.string.ticket_not_found)
            } else {
                getString(R.string.status_updated, ticketId, status)
            }
        }
    }

    private fun formatDate(millis: Long): String {
        return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(millis))
    }

    private fun formatReportCard(report: InfrastructureReport): String {
        return getString(
            R.string.report_card,
            report.ticketId,
            report.issueType,
            report.severity,
            report.description.ifBlank { getString(R.string.no_description) },
            report.status,
            formatDate(report.createdAtMillis),
            report.photoPath
        )
    }

    private fun renderAllReports(reports: List<InfrastructureReport>) {
        binding.allReportsList.removeAllViews()
        binding.clearReportsButton.isEnabled = reports.isNotEmpty()

        if (reports.isEmpty()) {
            binding.allReportsList.addView(reportTextView(getString(R.string.no_reports_yet)))
            return
        }

        reports.forEach { report ->
            binding.allReportsList.addView(reportCardView(report))
        }
    }

    private fun reportCardView(report: InfrastructureReport): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.result_box)
            setPadding(dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }
        }

        val photoFile = File(report.photoPath)
        val photoView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(190)
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundResource(R.drawable.report_photo_frame)
            contentDescription = getString(R.string.report_photo)
            if (photoFile.exists()) {
                setImageURI(Uri.fromFile(photoFile))
            } else {
                setImageResource(R.drawable.ic_camera)
                setPadding(dp(56))
            }
        }

        val detailsView = reportTextView(formatReportCard(report)).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
            }
            setTypeface(typeface, Typeface.NORMAL)
        }

        card.addView(photoView)
        card.addView(detailsView)
        return card
    }

    private fun confirmClearAllReports() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.clear_all_reports)
            .setMessage(R.string.clear_all_reports_confirm)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.clear) { _, _ ->
                clearAllReports()
            }
            .show()
    }

    private fun clearAllReports() {
        lifecycleScope.launch {
            val reports = withContext(Dispatchers.IO) {
                val currentReports = reportDao.observeReportsSnapshot()
                currentReports.forEach { report ->
                    File(report.photoPath).delete()
                }
                reportDao.deleteAllReports()
                currentReports
            }
            binding.ticketInput.text?.clear()
            binding.ticketResult.text = getString(R.string.enter_ticket_to_track)
            val deletedCount = reports.size
            android.widget.Toast.makeText(
                this@TrackerActivity,
                resources.getQuantityString(R.plurals.reports_cleared, deletedCount, deletedCount),
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun reportTextView(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            setTextAppearance(R.style.TextAppearance_NammaRaste_Body)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val ADMIN_ID = "admin"
        private const val ADMIN_PASSWORD = "1234"
    }
}
