package com.example.attit.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_logs")
@androidx.annotation.Keep
@Immutable // <--- KEEPS HISTORY LIST SMOOTH
data class AttendanceLog(
    @PrimaryKey(autoGenerate = true) val logId: Int = 0,
    val subjectId: Int,
    val timestamp: Long,
    val status: String
)