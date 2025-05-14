package com.example.purrytify.data.repository

import android.app.Application
import android.util.Log
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.db.dao.SongsDao
import com.example.purrytify.db.dao.UsersDao
import com.example.purrytify.db.entity.Songs
import com.example.purrytify.utils.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date
import com.example.purrytify.data.model.OnlineSongResponse


class DailyPlaylistRepository private constructor(
    private val application: Application,
    private val songsDao: SongsDao,
    private val usersDao: UsersDao,
    private val recommendationRepository: RecommendationRepository,
    private val authRepository: AuthRepository
) {
    private val TAG = "DailyPlaylistRepo"
    
    private val _dailyPlaylist = MutableStateFlow<List<Songs>>(emptyList())
    val dailyPlaylist: StateFlow<List<Songs>> = _dailyPlaylist
    
    private var hasPerformedRefreshCheck = false
    
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    init {
        resetRefreshCheckAtMidnight()
    }
    

    private fun resetRefreshCheckAtMidnight() {
        repositoryScope.launch {
            while(true) {
                val delayMillis = DateUtils.getMillisUntilMidnight()
                delay(delayMillis)
                
                hasPerformedRefreshCheck = false
                _dailyPlaylist.value = emptyList() // Clear the cached playlist
                Log.d(TAG, "Refresh check reset at midnight")
            }
        }
    }
    

    suspend fun getDailyPlaylist(forceRefresh: Boolean = false, region: String = "GLOBAL"): List<Songs> {
        val userId = authRepository.currentUserId ?: return emptyList()
        
        // Cached
        if (!forceRefresh && _dailyPlaylist.value.isNotEmpty() && hasPerformedRefreshCheck) {
            Log.d(TAG, "Using cached daily playlist from repository")
            return _dailyPlaylist.value
        }
        
        val lastFetchDate = usersDao.getDailyPlaylistLastFetchedDate(userId)
        val needsRefresh = DateUtils.isRefreshNeeded(lastFetchDate) || forceRefresh
        
        if (needsRefresh || _dailyPlaylist.value.isEmpty()) {
            Log.d(TAG, "Refreshing daily playlist for user $userId")
            val newPlaylist = recommendationRepository.getDailyPlaylist(userId, region)
            _dailyPlaylist.value = newPlaylist
            
            usersDao.updateDailyPlaylistLastFetched(userId, Date())
        } else {
            Log.d(TAG, "Using daily playlist from database")
        }
        
        hasPerformedRefreshCheck = true
        return _dailyPlaylist.value
    }
    
    fun convertToOnlineSongResponse(songs: List<Songs>, region: String): List<OnlineSongResponse> {
        return songs.mapIndexed { index, song ->
            OnlineSongResponse(
                id = song.id ?: 0,
                title = song.name ?: "",
                artist = song.artist ?: "",
                duration = song.duration?.toString() ?: "0:00",
                url = song.filePath ?: "",
                artwork = song.artwork ?: "",
                country = region,
                createdAt = "",
                updatedAt = "",
                rank = index + 1
            )
        }
    }
    
    fun invalidateCache() {
        hasPerformedRefreshCheck = false
    }
    
    companion object {
        @Volatile
        private var INSTANCE: DailyPlaylistRepository? = null
        
        fun getInstance(
            application: Application,
            songsDao: SongsDao,
            usersDao: UsersDao,
            recommendationRepository: RecommendationRepository,
            authRepository: AuthRepository
        ): DailyPlaylistRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = DailyPlaylistRepository(
                    application,
                    songsDao,
                    usersDao,
                    recommendationRepository,
                    authRepository
                )
                INSTANCE = instance
                instance
            }
        }
    }
}
