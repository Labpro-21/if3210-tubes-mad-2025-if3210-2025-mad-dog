package com.example.purrytify.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.purrytify.db.entity.Songs
import com.example.purrytify.db.entity.Users
import java.util.Date

@Entity(
    tableName = "listening_activity",
    foreignKeys = [
        ForeignKey(
            entity = Users::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Songs::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"]), Index(value = ["songId"])]
)
data class ListeningActivity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val songId: Int,
    val startTime: Date,
    val endTime: Date?,  // Null jika masih didengarkan
    val duration: Long = 0, // Durasi dalam milidetik
    val completed: Boolean = false // Menandakan apakah lagu selesai didengarkan
)