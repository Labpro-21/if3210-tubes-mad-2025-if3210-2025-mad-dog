package com.example.purrytify.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "day_streak_songs",
    foreignKeys = [
        ForeignKey(
            entity = SoundCapsule::class,
            parentColumns = ["id"],
            childColumns = ["capsuleId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Songs::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["capsuleId"]),
        Index(value = ["songId"]),
        Index(value = ["capsuleId", "date"], unique = true)
    ]
)
data class DayStreakSong(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val capsuleId: Int,
    val songId: Int?,
    val date: String,
    val playCount: Int
) 