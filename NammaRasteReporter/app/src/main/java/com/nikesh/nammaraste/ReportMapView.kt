package com.nikesh.nammaraste

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.nikesh.nammaraste.data.InfrastructureReport
import kotlin.math.max
import kotlin.math.min

class ReportMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var reports: List<InfrastructureReport> = emptyList()

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(241, 247, 245)
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(207, 224, 218)
        strokeWidth = 2f
    }
    private val roadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(230, 238, 234)
        strokeWidth = 18f
        strokeCap = Paint.Cap.ROUND
    }
    private val potholePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(217, 120, 40)
    }
    private val streetlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(42, 111, 219)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(16, 33, 28)
        textSize = 28f
        isFakeBoldText = true
    }
    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(91, 107, 102)
        textSize = 24f
    }

    fun setReports(newReports: List<InfrastructureReport>) {
        reports = newReports
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bounds = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(bounds, 28f, 28f, backgroundPaint)
        drawGrid(canvas)
        drawRoads(canvas)

        if (reports.isEmpty()) {
            canvas.drawText(context.getString(R.string.no_location_reports), 36f, height / 2f, textPaint)
            return
        }

        val coordinates = reports.mapNotNull { report ->
            val latitude = report.latitude
            val longitude = report.longitude
            if (latitude == null || longitude == null) null else latitude to longitude
        }
        val minLat = coordinates.minOf { it.first }
        val maxLat = coordinates.maxOf { it.first }
        val minLon = coordinates.minOf { it.second }
        val maxLon = coordinates.maxOf { it.second }

        reports.forEachIndexed { index, report ->
            val latitude = report.latitude ?: return@forEachIndexed
            val longitude = report.longitude ?: return@forEachIndexed
            val x = normalize(longitude, minLon, maxLon, 40f, width - 40f)
            val y = normalize(latitude, minLat, maxLat, height - 62f, 58f)
            val markerPaint = if (report.issueType.equals("Broken Streetlight", ignoreCase = true)) {
                streetlightPaint
            } else {
                potholePaint
            }
            canvas.drawCircle(x, y, 18f, markerPaint)
            canvas.drawCircle(x, y, 24f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = markerPaint.color
                alpha = 55
            })
            canvas.drawText((index + 1).toString(), x + 24f, y + 8f, textPaint)
        }

        drawLegend(canvas)
    }

    private fun drawGrid(canvas: Canvas) {
        val columns = 4
        val rows = 5
        for (i in 1 until columns) {
            val x = width * i / columns.toFloat()
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
        }
        for (i in 1 until rows) {
            val y = height * i / rows.toFloat()
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
        }
    }

    private fun drawRoads(canvas: Canvas) {
        canvas.drawLine(20f, height * 0.28f, width - 20f, height * 0.68f, roadPaint)
        canvas.drawLine(width * 0.18f, 20f, width * 0.76f, height - 20f, roadPaint)
        canvas.drawLine(20f, height * 0.78f, width - 20f, height * 0.42f, roadPaint)
    }

    private fun drawLegend(canvas: Canvas) {
        val y = height - 24f
        canvas.drawCircle(34f, y, 10f, potholePaint)
        canvas.drawText(context.getString(R.string.pothole_label), 52f, y + 8f, smallTextPaint)
        canvas.drawCircle(192f, y, 10f, streetlightPaint)
        canvas.drawText(context.getString(R.string.streetlight_label), 210f, y + 8f, smallTextPaint)
    }

    private fun normalize(
        value: Double,
        minValue: Double,
        maxValue: Double,
        minOutput: Float,
        maxOutput: Float
    ): Float {
        if (maxValue == minValue) return (minOutput + maxOutput) / 2f
        val ratio = ((value - minValue) / (maxValue - minValue)).toFloat()
        return min(maxOutput, max(minOutput, minOutput + ratio * (maxOutput - minOutput)))
    }
}
