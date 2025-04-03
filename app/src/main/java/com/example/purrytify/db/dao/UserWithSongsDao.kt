package com.example.purrytify.db.dao
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.example.purrytify.db.relationship.UserWithSongs
import kotlinx.coroutines.flow.Flow
@Dao
interface UserWithSongsDao {

    @Transaction
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserWithSongs(userId: Int): Flow<List<UserWithSongs>>

    @Transaction
    @Query("SELECT * FROM users")
    fun getAllUsersWithSongs(): Flow<List<UserWithSongs>>
}