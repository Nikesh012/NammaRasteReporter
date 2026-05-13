package com.nikesh.nammaraste.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(report: InfrastructureReport)

    @Query("SELECT * FROM infrastructure_reports WHERE ticketId = :ticketId LIMIT 1")
    suspend fun findByTicketId(ticketId: String): InfrastructureReport?

    @Query("SELECT * FROM infrastructure_reports ORDER BY createdAtMillis DESC")
    fun observeReports(): Flow<List<InfrastructureReport>>

    @Query("SELECT * FROM infrastructure_reports ORDER BY createdAtMillis DESC")
    suspend fun observeReportsSnapshot(): List<InfrastructureReport>

    @Query("DELETE FROM infrastructure_reports")
    suspend fun deleteAllReports()

    @Query("UPDATE infrastructure_reports SET status = :status WHERE ticketId = :ticketId")
    suspend fun updateStatus(ticketId: String, status: String): Int
}
