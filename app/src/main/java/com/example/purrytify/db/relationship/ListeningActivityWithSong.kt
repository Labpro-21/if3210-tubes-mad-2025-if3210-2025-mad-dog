package com.example.purrytify.db.relationship

import androidx.room.Embedded
import androidx.room.Relation
import com.example.purrytify.db.entity.ListeningActivity
import com.example.purrytify.db.entity.Songs

data class ListeningActivityWithSong(
    @Embedded val listeningActivity:ListeningActivity,
    @Relation(
        parentColumn = "songId",
        entityColumn = "id"
    )
    val song: Songs
)