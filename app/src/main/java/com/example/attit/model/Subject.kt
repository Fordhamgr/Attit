package com.example.attit.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.DocumentId

@Entity(tableName = "subjects")
@androidx.annotation.Keep
@Immutable
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "",
    val attended: Int = 0,
    val total: Int = 0,
    val goal: Int = 75,

    // --- MOVE IT HERE 👇 ---
    // This works because Firestore uses the constructor to set it.
    @DocumentId val firebaseId: String? = null,

    val unique: String = ""
) {
    // --- Helper Functions (Logic) ---

    @get:Exclude
    val percentage: Float
        get() = if (total == 0) 0f else (attended.toFloat() / total.toFloat()) * 100

    @Exclude
    fun getStatusColor(): Color {
        return if (percentage >= goal) Color(0xFF4CAF50)
        else Color(0xFFE53935)
    }

    @Exclude
    fun getStatusText(): String {
        return if (percentage >= goal) {
            "You are on track! Keep it up."
        } else {
            val needed = calculateClassesNeeded()
            "Attend next $needed classes to reach $goal%."
        }
    }

    private fun calculateClassesNeeded(): Int {
        var currentAttended = attended
        var currentTotal = total
        var classesNeeded = 0

        while (currentTotal == 0 || (currentAttended.toFloat() / currentTotal.toFloat() * 100) < goal) {
            currentAttended++
            currentTotal++
            classesNeeded++
        }
        return classesNeeded
    }
}