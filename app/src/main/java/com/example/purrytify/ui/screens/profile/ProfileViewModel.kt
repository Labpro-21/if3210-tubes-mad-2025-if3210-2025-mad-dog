package com.example.purrytify.ui.screens.profile
import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.auth.ProfileRepository
import com.example.purrytify.data.model.ProfileResponse
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProfileRepository.getInstance(application)

    private val _profile = MutableStateFlow<ProfileResponse?>(null)
    val profile: StateFlow<ProfileResponse?> = _profile

    private val _songsCount = MutableStateFlow(0)
    val songsCount: StateFlow<Int> = _songsCount

    private val _playedCount = MutableStateFlow(0)
    val playedCount: StateFlow<Int> = _playedCount

    private val _favoriteCount = MutableStateFlow(0)
    val favoriteCount: StateFlow<Int> = _favoriteCount




    fun getProfile() {
        viewModelScope.launch {
            val result = repository.getProfile()
            _profile.value = result
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