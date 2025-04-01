package com.example.purrytify.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Songs(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val artist: String,
    val description: String,
    val isFavorite: Boolean = false,
    val filePath: String, // Path ke file audio
    val artwork: String? = null, // Path atau URL ke artwork lagu
    val duration: Long // Durasi lagu dalam milidetik
)
