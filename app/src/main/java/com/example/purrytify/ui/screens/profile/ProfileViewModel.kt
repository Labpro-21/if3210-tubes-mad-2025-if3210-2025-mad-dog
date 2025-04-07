package com.example.purrytify.ui.screens.profile
import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.auth.ProfileRepository
import com.example.purrytify.data.model.ProfileResponse
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application){
    private val repository = ProfileRepository.getInstance(application)
    var profile by mutableStateOf<ProfileResponse?>(null)
    private set

    var songsCount by androidx.compose.runtime.mutableStateOf(0)
        private set

    var favoriteCount by androidx.compose.runtime.mutableStateOf(0)
        private set

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            profile = repository.getProfile()
            songsCount = repository.getSongsCount()
            favoriteCount = repository.getSongsLiked()
        }
    }
}