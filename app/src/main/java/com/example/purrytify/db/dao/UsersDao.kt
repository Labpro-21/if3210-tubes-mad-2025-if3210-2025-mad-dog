package com.example.purrytify.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.purrytify.db.entity.Users
import kotlinx.coroutines.flow.Flow
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

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<Users>>
}
