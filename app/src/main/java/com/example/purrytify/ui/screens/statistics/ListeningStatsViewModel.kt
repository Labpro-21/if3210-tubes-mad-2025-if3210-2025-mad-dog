package com.example.purrytify.ui.screens.statistics

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.data.repository.ListeningActivityRepository
import com.example.purrytify.db.AppDatabase
import com.example.purrytify.db.dao.ListeningActivityDao.DailyListeningStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ListeningStatsViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository.getInstance(application)
    private val listenActivityDao = AppDatabase.getDatabase(application).listeningCapsuleDao()
    private val repository = ListeningActivityRepository.getInstance(listenActivityDao)

    private val TAG = "ListeningStatsViewModel"

    private val _dailyStats = MutableStateFlow<List<DailyListeningStats>>(emptyList())
    val dailyStats: StateFlow<List<DailyListeningStats>> = _dailyStats

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _monthYear = MutableStateFlow<String?>(null)
    val monthYear: StateFlow<String?> = _monthYear

    private val _averageMinutesPerDay = MutableStateFlow(0.0)
    val averageMinutesPerDay: StateFlow<Double> = _averageMinutesPerDay

    fun loadDailyStats() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = authRepository.currentUserId
                if (userId != null) {
                    val stats = repository.getDailyListeningData(userId)
                    Log.d(TAG,"Stats: $stats")
                    _dailyStats.value = stats
                    
                    // Calculate average
                    if (stats.isNotEmpty()) {
                        val totalMinutes = stats.sumOf { it.totalMinutes }
                        _averageMinutesPerDay.value = totalMinutes / stats.size
                    }

                    // Set month and year
                    val currentDate = LocalDate.now()
                    _monthYear.value = currentDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                }
            } catch (e: Exception) {
                // Handle error
                _dailyStats.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadDailyStats(year: Int = LocalDate.now().year, month: Int = LocalDate.now().monthValue) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = authRepository.currentUserId
                if (userId != null) {
                    val stats = repository.getDailyListeningData(userId, year, month)
                    Log.d(TAG,"Stats for $month/$year: $stats")
                    _dailyStats.value = stats
                    
                    // Calculate average
                    if (stats.isNotEmpty()) {
                        val totalMinutes = stats.sumOf { it.totalMinutes }
                        _averageMinutesPerDay.value = totalMinutes / stats.size
                    } else {
                        _averageMinutesPerDay.value = 0.0
                    }

                    // Set month and year from parameters
                    val date = LocalDate.of(year, month, 1)
                    _monthYear.value = date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                }
            } catch (e: Exception) {
                // Handle error
                Log.e(TAG, "Error loading stats: ${e.message}", e)
                _dailyStats.value = emptyList()
                _averageMinutesPerDay.value = 0.0
            } finally {
                _isLoading.value = false
            }
        }
    }
} 