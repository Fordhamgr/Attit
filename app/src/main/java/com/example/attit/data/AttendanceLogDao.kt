package com.example.attit.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.attit.model.AttendanceLog
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceLogDao {
    // Get logs for ONE specific subject, ordered by newest first
    @Query("SELECT * FROM attendance_logs WHERE subjectId = :subjectId ORDER BY timestamp DESC")
    fun getLogsForSubject(subjectId: Int): Flow<List<AttendanceLog>>

    @Insert
    suspend fun insertLog(log: AttendanceLog)

    // Optional: Delete logs if a subject is deleted (We can add this later)
    @Query("DELETE FROM attendance_logs WHERE subjectId = :subjectId")
    suspend fun deleteLogsForSubject(subjectId: Int)

    @Delete
    suspend fun deleteLog(log: AttendanceLog)

    // ... existing queries ...

    // --- ADD THIS FOR QUICK UNDO ---
    @Query("SELECT * FROM attendance_logs WHERE subjectId = :subjectId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastLog(subjectId: Int): AttendanceLog?

    // Add this specifically for the Sync process
    @Query("SELECT * FROM attendance_logs WHERE subjectId = :subjectId")
    suspend fun getLogsSync(subjectId: Int): List<AttendanceLog>

}