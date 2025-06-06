package com.example.purrytify.ui.screens.topartist

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.data.repository.ListeningActivityRepository
import com.example.purrytify.db.AppDatabase
import com.example.purrytify.db.dao.ListeningActivityDao.MonthlyArtistStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TopArtistViewModel(application: Application) : AndroidViewModel(application) {
    private val listenActivityDao = AppDatabase.getDatabase(application).listeningCapsuleDao()
    private val repository = ListeningActivityRepository.getInstance(listenActivityDao)
    private val _topArtists = MutableStateFlow<List<MonthlyArtistStats>>(emptyList())
    private val authRepository = AuthRepository.getInstance(application)

    val topArtists: StateFlow<List<MonthlyArtistStats>> = _topArtists

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val TAG = "TopArtistViewModel"
    
    private val _displayMonthYear = MutableStateFlow<String?>(null)
    val displayMonthYear: StateFlow<String?> = _displayMonthYear

    init {
        loadTopArtists()
    }

    fun loadTopArtists() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val userId = authRepository.currentUserId
                if(userId != null){
                    val artists = repository.getMonthlyArtistsStats(userId)
                    Log.d(TAG,"Artists: $artists")

                    _topArtists.value = artists
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load top artists"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadTopArtists(year: Int = java.time.LocalDate.now().year, month: Int = java.time.LocalDate.now().monthValue) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val userId = authRepository.currentUserId
                if (userId != null) {
                    // Update the display month and year
                    val date = java.time.LocalDate.of(year, month, 1)
                    _displayMonthYear.value = date.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))
                    
                    // Get artists for specific month and year
                    val artists = repository.getMonthlyArtistsStats(userId, year, month)
                    Log.d(TAG, "Artists for $month/$year: ${artists.size}")
                    
                    _topArtists.value = artists
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load top artists"
                Log.e(TAG, "Error loading artists: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
} 