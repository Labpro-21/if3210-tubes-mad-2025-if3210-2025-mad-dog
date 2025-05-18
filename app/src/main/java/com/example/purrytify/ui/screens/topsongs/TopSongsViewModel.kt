package com.example.purrytify.ui.screens.topsongs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.data.repository.ListeningActivityRepository
import com.example.purrytify.db.AppDatabase
import com.example.purrytify.db.dao.ListeningActivityDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TopSongsViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository.getInstance(application)
    private val listenActivityDao = AppDatabase.getDatabase(application).listeningCapsuleDao()
    private val listenActivityRepository = ListeningActivityRepository.getInstance(listenActivityDao)

    private val _topSongs = MutableStateFlow<List<ListeningActivityDao.MonthlyPlayedSong>>(emptyList())
    val topSongs: StateFlow<List<ListeningActivityDao.MonthlyPlayedSong>> = _topSongs

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadTopSongs()
    }

    private fun loadTopSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = authRepository.currentUserId
                if (userId != null) {
                    val songs = listenActivityRepository.getMonthlyPlayedSongs(userId)
                    _topSongs.value = songs
                }
            } catch (e: Exception) {
                // Handle error if needed
            } finally {
                _isLoading.value = false
            }
        }
    }
} 