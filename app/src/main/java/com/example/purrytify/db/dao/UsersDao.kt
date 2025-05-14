package com.example.purrytify.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.purrytify.db.entity.Users
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface UsersDao {

    @Insert
    suspend fun insertUser(user: Users): Long

    @Update
    suspend fun updateUser(user: Users)

    @Delete
    suspend fun deleteUser(user: Users)

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: Int): Users?

    @Query("SELECT totalplayed FROM users WHERE id =:userId")
    suspend fun getTotalPlayedById(userId: Int?): Int?


    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<Users>>

    @Query("UPDATE users SET totalplayed = totalplayed + 1 WHERE id = :userId")
    suspend fun incrementTotalPlayed(userId: Int)

    @Query("UPDATE users SET dailyPlaylistLastFetched = :date WHERE id = :userId")
    suspend fun updateDailyPlaylistLastFetched(userId: Int, date: Date)

    @Query("SELECT dailyPlaylistLastFetched FROM users WHERE id = :userId")
    suspend fun getDailyPlaylistLastFetchedDate(userId: Int): Date?

}