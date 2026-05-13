package com.nikesh.nammaraste.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "infrastructure_reports")
data class InfrastructureReport(
    @PrimaryKey val ticketId: String,
    val issueType: String,
    val severity: String,
    val description: String,
    val photoPath: String,
    val latitude: Double?,
    val longitude: Double?,
    val createdAtMillis: Long,
    val status: String = "Submitted"
)
