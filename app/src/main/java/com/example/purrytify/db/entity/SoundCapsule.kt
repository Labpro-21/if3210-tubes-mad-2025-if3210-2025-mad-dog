package com.example.purrytify.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.purrytify.db.entity.Users

@Entity(
    tableName = "sound_capsules",
    foreignKeys = [ForeignKey(
        entity = Users::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["userId", "year", "month"], unique = true)]
)
data class SoundCapsule(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val year: Int,
    val month: Int,
    val totalTimeListened: Long,
    val topArtistName: String?,
    val topArtistPlayCount: Int?,
    val topSongId: Int?,
    val listeningDayStreak: Int,
    val lastUpdated: Long = System.currentTimeMillis()
) 