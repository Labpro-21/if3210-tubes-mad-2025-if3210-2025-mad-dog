package com.example.purrytify.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class Users(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val totalPlayed: Int = 0,
    val monthlyListeningTime: Long = 0, // Total waktu dengarkan dalam milidetik bulan ini
    val lastUpdatedMonth: Int = 0 // Bulan terakhir data diperbarui
)