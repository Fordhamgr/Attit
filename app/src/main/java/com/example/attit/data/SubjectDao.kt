package com.example.attit.data

import androidx.room.*
import com.example.attit.model.Subject
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    // 1. Used for the Home Screen list (Live updates)
    @Query("SELECT * FROM subjects ORDER BY id DESC")
    fun getAllSubjects(): Flow<List<Subject>>

    // --- UNDO LOGIC ---
    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubjectById(id: Int): Subject?

    // --- SYNC LOGIC ---
    @Query("SELECT * FROM subjects")
    suspend fun getAllSubjectsOnce(): List<Subject>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Update
    suspend fun updateSubject(subject: Subject)

    @Delete
    suspend fun deleteSubject(subject: Subject)
}