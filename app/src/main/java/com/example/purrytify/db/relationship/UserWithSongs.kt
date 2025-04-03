package com.example.purrytify.db.relationship

import androidx.room.Embedded
import androidx.room.Relation
import com.example.purrytify.db.entity.Songs
import com.example.purrytify.db.entity.Users

data class UserWithSongs(
    @Embedded val user: Users,
    @Relation(
        parentColumn = "id",
        entityColumn = "userId"
    )
    val songs: List<Songs>
)
