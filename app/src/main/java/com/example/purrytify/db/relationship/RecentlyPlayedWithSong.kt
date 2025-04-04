package com.example.purrytify.db.relationship
import androidx.room.Embedded
import androidx.room.Relation
import com.example.purrytify.db.entity.Songs
import com.example.purrytify.db.entity.RecentlyPlayed
data class RecentlyPlayedWithSong(
    @Embedded val recentlyPlayed: RecentlyPlayed,
    @Relation(
        parentColumn = "songId",
        entityColumn = "id",
        entity = Songs::class
    )
    val song: Songs
)