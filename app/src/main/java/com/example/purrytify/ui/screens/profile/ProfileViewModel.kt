package com.example.purrytify.ui.screens.profile
import NetworkMonitor
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.auth.ProfileRepository
import com.example.purrytify.data.model.ProfileResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProfileRepository.getInstance(application)
    private val networkMonitor = NetworkMonitor

    private val _profile = MutableStateFlow<ProfileResponse?>(null)
    val profile: StateFlow<ProfileResponse?> = _profile

    private val _songsCount = MutableStateFlow(0)
    val songsCount: StateFlow<Int> = _songsCount

    private val _playedCount = MutableStateFlow(0)
    val playedCount: StateFlow<Int> = _playedCount

    private val _favoriteCount = MutableStateFlow(0)
    val favoriteCount: StateFlow<Int> = _favoriteCount

    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _noInternet = MutableStateFlow(false)
    val noInternet: StateFlow<Boolean> = _noInternet

    fun getProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _isError.value = false
            _noInternet.value = false

            if (networkMonitor.isConnected.first()) {
                val result = repository.getProfile()
                if (result != null) {
                    _profile.value = result
                } else {
                    _isError.value = true
                }
            } else {
                _noInternet.value = true
            }
            _isLoading.value = false
        }
    }

    fun getSongsCount() {
        viewModelScope.launch {
            _songsCount.value = repository.getSongsCount()
        }
    }

    fun getFavoriteSongsCount() {
        viewModelScope.launch {
            _favoriteCount.value = repository.getSongsLiked()
        }
    }

    fun getTotalListenedCount() {
        viewModelScope.launch {
            _playedCount.value = repository.getTotalListened()
        }
    }
}