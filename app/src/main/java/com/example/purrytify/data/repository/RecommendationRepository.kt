package com.example.purrytify.data.repository

import android.content.Context
import com.example.purrytify.db.dao.SongsDao
import com.example.purrytify.db.entity.Songs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.Date

class RecommendationRepository(
    private val songsDao: SongsDao,
    private val context: Context,
    private val onlineSongRepository: OnlineSongRepository
)
{
    suspend fun getDailyPlaylist(userId: Int, limit: Int = 20): List<Songs> = withContext(Dispatchers.IO){
        val localSongs = songsDao.getAllSongsForUser(userId).firstOrNull() ?: emptyList()
        val onlineSongs = onlineSongRepository.getTopGlobalSongs() ?: emptyList()

        val onlineAsLocal = onlineSongs.map { online ->
            Songs(
                id = online.id,
                name = online.title,
                artist = online.artist,
                description = online.country,
                filePath = online.url,
                artwork = online.artwork,
                duration = com.example.purrytify.utils.MediaUtils.parseDuration(online.duration),
                userId = userId,
                uploadDate = Date()
            )
        }

        val combined = (localSongs + onlineAsLocal).shuffled().take(limit)
        combined
    }

}