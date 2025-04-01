package com.example.purrytify.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.purrytify.db.entity.Songs
import kotlinx.coroutines.flow.Flow

@Dao
interface SongsDao {

    // Insert a song (will automatically assign ID if autoGenerate is true)
    @Insert
    suspend fun insert(song: Songs)

    // Insert a list of songs
    @Insert
    suspend fun insertAll(songs: List<Songs>)

    // Update an existing song
    @Update
    suspend fun update(song: Songs)

    // Get all songs as a Flow
    @Query("SELECT * FROM songs")
    fun getAllSongs(): Flow<List<Songs>>

    // Get all songs that are marked as favorite as a Flow
    @Query("SELECT * FROM songs WHERE isFavorite = 1")
    fun getFavoriteSongs(): Flow<List<Songs>>

    // Get a song by its ID
    @Query("SELECT * FROM songs WHERE id = :songId LIMIT 1")
    suspend fun getSongById(songId: Int): Songs?

    // Delete a song by its ID
    @Query("DELETE FROM songs WHERE id = :songId")
    suspend fun deleteById(songId: Int)

    // Delete all songs
    @Query("DELETE FROM songs")
    suspend fun deleteAllSongs()

    // Update the favorite status of a song
    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :songId")
    suspend fun updateFavoriteStatus(songId: Int, isFavorite: Boolean)
}
