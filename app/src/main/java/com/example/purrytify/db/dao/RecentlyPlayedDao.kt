package com.example.purrytify.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.purrytify.db.entity.RecentlyPlayed
import com.example.purrytify.db.relationship.RecentlyPlayedWithSong
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentlyPlayedDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentlyPlayed(recentlyPlayed: RecentlyPlayed)

    @Query("SELECT * FROM recently_played WHERE userId = :userId ORDER BY playedAt DESC")
    fun getRecentlyPlayedSongsForUser(userId: Int): Flow<List<RecentlyPlayedWithSong>>
}