package com.example.purrytify.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.purrytify.db.entity.Songs
import kotlinx.coroutines.flow.Flow

@Dao
interface SongsDao {

    @Insert
    suspend fun insertSong(song: Songs): Long

    @Update
    suspend fun updateSong(song: Songs)

    @Delete
    suspend fun deleteSong(song: Songs)

    @Query("SELECT * FROM songs WHERE id = :songId")
    suspend fun getSongById(songId: Int): Songs?

    @Query("SELECT * FROM songs WHERE userId = :userId")
    fun getAllSongsForUser(userId: Int): Flow<List<Songs>>

    @Query("SELECT * FROM songs")
    fun getAllSongs(): Flow<List<Songs>>

    @Query("SELECT * FROM songs WHERE userId = :userId AND isFavorite = 1")
    fun getFavoriteSongsForUser(userId: Int): Flow<List<Songs>>

    @Query("DELETE FROM songs WHERE id = :songId")
    suspend fun deleteById(songId: Int)


    @Query("SELECT * FROM songs WHERE name LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%'")
    fun searchSongs(query: String): Flow<List<Songs>>

    @Query("SELECT * FROM songs WHERE isFavorite = 1")
    fun getFavoriteSongs(): Flow<List<Songs>>

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :songId")
    suspend fun updateFavoriteStatus(songId: Int, isFavorite: Boolean)

    @Query("SELECT * FROM songs WHERE userId = :userId ORDER BY name ASC")
    fun getSongsForUserSortedByName(userId: Int): Flow<List<Songs>>

    @Query("SELECT * FROM songs WHERE userId = :userId ORDER BY artist ASC")
    fun getSongsForUserSortedByArtist(userId: Int): Flow<List<Songs>>

    @Query("SELECT * FROM songs WHERE userId = :userId ORDER BY duration ASC")
    fun getSongsForUserSortedByDuration(userId: Int): Flow<List<Songs>>
}