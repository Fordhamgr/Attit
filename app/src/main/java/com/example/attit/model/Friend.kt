package com.example.attit.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friends")
@androidx.annotation.Keep
data class Friend(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nickname: String,
    val email: String,
    val uid: String,
    val username: String? = null,
    val avatarId: String? = null // <--- NEW FIELD
)