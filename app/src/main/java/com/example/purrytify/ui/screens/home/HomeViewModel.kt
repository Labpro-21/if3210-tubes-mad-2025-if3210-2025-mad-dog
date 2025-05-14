package com.example.purrytify.ui.screens.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.data.repository.DailyPlaylistRepository
import com.example.purrytify.data.repository.OnlineSongRepository
import com.example.purrytify.data.repository.RecommendationRepository
import com.example.purrytify.db.AppDatabase
import com.example.purrytify.db.entity.Songs
import com.example.purrytify.db.relationship.RecentlyPlayedWithSong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val recentlyPlayedDao = AppDatabase.getDatabase(application).recentlyPlayedDao()
    private val songsDao = AppDatabase.getDatabase(application).songsDao()
    private val usersDao = AppDatabase.getDatabase(application).usersDao()
    private val authRepository = AuthRepository.getInstance(application)
    private val Tag = "HomeViewModel"
    
    private val onlineSongRepository = OnlineSongRepository.getInstance(application)
    
    private val recommendationRepository = RecommendationRepository(
        AppDatabase.getDatabase(application).songsDao(),
        application,
        onlineSongRepository
    )
    
    private val dailyPlaylistRepository = DailyPlaylistRepository.getInstance(
        application,
        songsDao,
        usersDao,
        recommendationRepository,
        authRepository
    )
    
    val dailyPlaylist: StateFlow<List<Songs>> = dailyPlaylistRepository.dailyPlaylist

    val userId: Int?
        get() = authRepository.currentUserId

    val recentlyPlayedSongs: Flow<List<RecentlyPlayedWithSong>>
        get() {
            val userId = this.userId
            return if (userId != null) {
                recentlyPlayedDao.getRecentlyPlayedSongsForUser(userId)
            } else {
                emptyFlow()
            }
        }

    val newAddedSongs: Flow<List<Songs>>
        get() {
            val userId = this.userId
            return if (userId != null) {
                songsDao.getSongsForUserSortedByUploadDate(userId)
            } else {
                emptyFlow()
            }
        }

    init {
        if (userId == null) {
            Log.w(Tag, "User ID is null. Recently played songs will be empty.")
        }
        Log.d(Tag, "User ID: $userId, Recently played songs: $recentlyPlayedSongs")
    }

    fun loadDailyPlaylist() {
        viewModelScope.launch {
            val userId = authRepository.currentUserId ?: return@launch
            dailyPlaylistRepository.getDailyPlaylist()
        }
    }
}