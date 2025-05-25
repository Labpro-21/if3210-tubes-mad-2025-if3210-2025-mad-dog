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
    
    private val _monthYear = MutableStateFlow<String?>(null)
    val monthYear: StateFlow<String?> = _monthYear
    
    private val TAG = "TopSongsViewModel"

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

    fun loadTopSongs(year: Int = java.time.LocalDate.now().year, month: Int = java.time.LocalDate.now().monthValue) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = authRepository.currentUserId
                if (userId != null) {
                    // Update the monthYear display
                    val date = java.time.LocalDate.of(year, month, 1)
                    _monthYear.value = date.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))
                    
                    // Get songs for specific month and year
                    val songs = listenActivityRepository.getMonthlyPlayedSongs(userId, year, month)
                    _topSongs.value = songs
                    
                    android.util.Log.d(TAG, "Loaded ${songs.size} top songs for $month/$year")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error loading top songs: ${e.message}", e)
                _topSongs.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
} 