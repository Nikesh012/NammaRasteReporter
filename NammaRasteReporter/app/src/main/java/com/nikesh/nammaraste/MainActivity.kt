package com.nikesh.nammaraste

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.nikesh.nammaraste.data.InfrastructureReport
import com.nikesh.nammaraste.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null

    private val reportDao by lazy {
        (application as NammaRasteApplication).database.reportDao()
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true) {
            startCamera()
        } else {
            Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupLogin()
        setupReportForm()
        setupTrackerShortcut()
        observeReports()
        requestPermissions()
    }

    private fun setupLogin() {
        val preferences = getSharedPreferences("login", Context.MODE_PRIVATE)
        val savedName = preferences.getString("user_name", null)
        updateLoginState(savedName)

        binding.loginButton.setOnClickListener {
            val name = binding.nameInput.text?.toString()?.trim().orEmpty()
            if (name.length < 3) {
                binding.nameInput.error = getString(R.string.enter_valid_name)
                return@setOnClickListener
            }

            preferences.edit().putString("user_name", name).apply()
            updateLoginState(name)
        }

        binding.logoutButton.setOnClickListener {
            preferences.edit().remove("user_name").apply()
            binding.nameInput.text?.clear()
            binding.submissionResult.text = getString(R.string.waiting_for_report)
            updateLoginState(null)
            Toast.makeText(this, R.string.logged_out, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateLoginState(userName: String?) {
        val isLoggedIn = userName != null
        binding.loginPanel.visibility = if (isLoggedIn) View.GONE else View.VISIBLE
        binding.appContent.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
        binding.userNameText.text = if (isLoggedIn) {
            getString(R.string.logged_in_as, userName)
        } else {
            ""
        }
    }

    private fun setupReportForm() {
        binding.issueTypeInput.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                listOf("Pothole", "Broken Streetlight")
            )
        )
        binding.issueTypeInput.setText("Pothole", false)

        binding.severityInput.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                listOf("Low", "Medium", "High")
            )
        )
        binding.severityInput.setText("Medium", false)

        binding.captureButton.setOnClickListener { showIssueTypePicker() }
    }

    private fun showIssueTypePicker() {
        val issueTypes = arrayOf("Pothole", "Broken Streetlight")
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.choose_issue_type)
            .setItems(issueTypes) { _, selectedIndex ->
                val issueType = issueTypes[selectedIndex]
                binding.issueTypeInput.setText(issueType, false)
                captureReport(issueType)
            }
            .show()
    }

    private fun setupTrackerShortcut() {
        binding.openTrackerButton.setOnClickListener {
            startActivity(Intent(this, TrackerActivity::class.java))
        }
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
            }
        }
    }

    private fun requestPermissions() {
        val required = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val missing = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            startCamera()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (exception: Exception) {
                Toast.makeText(this, exception.localizedMessage, Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureReport(selectedIssueType: String) {
        val capture = imageCapture ?: run {
            Toast.makeText(this, R.string.camera_not_ready, Toast.LENGTH_SHORT).show()
            return
        }

        binding.captureButton.isEnabled = false
        val issueType = selectedIssueType.ifBlank { "Pothole" }
        val folderName = reportFolderName(issueType)
        val reportsDirectory = File(getExternalFilesDir("reports") ?: filesDir, folderName)
        reportsDirectory.mkdirs()
        val photoFile = File(reportsDirectory, "$folderName-report-${System.currentTimeMillis()}.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    saveReport(photoFile, issueType, folderName)
                }

                override fun onError(exception: ImageCaptureException) {
                    binding.captureButton.isEnabled = true
                    Toast.makeText(
                        this@MainActivity,
                        exception.localizedMessage ?: getString(R.string.capture_failed),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    private fun saveReport(photoFile: File, issueType: String, folderName: String) {
        val createdAt = System.currentTimeMillis()
        val location = getLastKnownLocation()
        val report = InfrastructureReport(
            ticketId = generateTicketId(createdAt),
            issueType = issueType,
            severity = binding.severityInput.text.toString(),
            description = binding.descriptionInput.text?.toString()?.trim().orEmpty(),
            photoPath = photoFile.absolutePath,
            latitude = location?.latitude,
            longitude = location?.longitude,
            createdAtMillis = createdAt
        )

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                reportDao.insert(report)
            }
            binding.captureButton.isEnabled = true
            binding.submissionResult.text = getString(
                R.string.submission_success,
                report.ticketId,
                report.issueType,
                report.description.ifBlank { getString(R.string.no_description) },
                folderName,
                locationLabel(report.latitude, report.longitude)
            )
        }
    }

    private fun reportFolderName(issueType: String): String {
        return when (issueType.lowercase(Locale.US)) {
            "broken streetlight" -> "streetlights"
            else -> "potholes"
        }
    }

    private fun getLastKnownLocation(): Location? {
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.getProviders(true)
            .mapNotNull { provider -> locationManager.getLastKnownLocation(provider) }
            .maxByOrNull { it.time }
    }

    private fun generateTicketId(createdAt: Long): String {
        val datePart = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(createdAt))
        val suffix = createdAt.toString().takeLast(6)
        return "NR-$datePart-$suffix"
    }

    private fun locationLabel(latitude: Double?, longitude: Double?): String {
        return if (latitude == null || longitude == null) {
            getString(R.string.location_unavailable)
        } else {
            "%.5f, %.5f".format(Locale.US, latitude, longitude)
        }
    }
}
