// SongDetailViewModel.kt

package com.example.purrytify.ui.screens.songdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.db.AppDatabase
import com.example.purrytify.db.entity.Songs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SongDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val songDao = AppDatabase.getDatabase(application).songsDao()

    private val _songDetails = MutableStateFlow<SongDetailUiState>(SongDetailUiState.Loading)
    val songDetails: StateFlow<SongDetailUiState> = _songDetails

    fun loadSongDetails(songId: Int) {
        viewModelScope.launch {
            try {
                val song = songDao.getSongById(songId)
                if (song != null) {
                    _songDetails.value = SongDetailUiState.Success(song)
                } else {
                    _songDetails.value = SongDetailUiState.Error("Song not found")
                }
            } catch (e: Exception) {
                _songDetails.value = SongDetailUiState.Error("Error loading song details: ${e.message}")
            }
        }
    }
    fun toggleFavoriteStatus(song: Songs) {
        viewModelScope.launch {
            songDao.updateSong(song.copy(isFavorite = !song.isFavorite))
            loadSongDetails(song.id)
        }
    }

    fun deleteSong(song: Songs) {
        viewModelScope.launch {
            songDao.deleteSong(song)
        }
    }
}