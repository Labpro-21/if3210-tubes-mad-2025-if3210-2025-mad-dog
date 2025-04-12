// SongDetailViewModel.kt

package com.example.purrytify.ui.screens.songdetail

import android.app.Application
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.db.AppDatabase
import com.example.purrytify.db.entity.RecentlyPlayed
import com.example.purrytify.db.entity.Songs
import com.example.purrytify.utils.MediaUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date

class SongDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val songDao = AppDatabase.getDatabase(application).songsDao()
    private val context = application.applicationContext
    private val recentlyPlayedDao = AppDatabase.getDatabase(application).recentlyPlayedDao()
    private val authRepository = AuthRepository.getInstance(application)

    private val _isUpdateSuccessful = MutableStateFlow(false)
    val isUpdateSuccessful: StateFlow<Boolean> = _isUpdateSuccessful

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
    fun insertRecentlyPlayed(songId: Int) {
        viewModelScope.launch {
            val userId = authRepository.currentUserId

            if (userId != null) {
                try {
                    val recentlyPlayed = RecentlyPlayed(
                        userId = userId,
                        songId = songId,
                        playedAt = Date()
                    )
                    recentlyPlayedDao.insertRecentlyPlayed(recentlyPlayed)
                    Log.d("Recently played added: ", recentlyPlayed.toString())
                } catch (e: Exception) {
                    Log.e("SongDetailViewModel", "Error inserting recently played: ${e.message}")
                    Toast.makeText(context, "Error saving recently played", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e("SongDetailViewModel", "User ID is null, cannot insert recently played")
                Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
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



    fun updateSong(song: Songs, photoUri: Uri?, fileUri: Uri?) {
        viewModelScope.launch {
            var artworkPath: String? = song.artwork
            var audioPath: String? = song.filePath
            var duration = song.duration

            if (photoUri != null) {
                artworkPath = photoUri.toString() // Simpan URI sebagai String
            }

            if (fileUri != null && isAudioFile(fileUri)) {
                audioPath = fileUri.toString() // Simpan URI sebagai String
                duration = MediaUtils.getAudioDuration(fileUri, context)
            }

            val updatedSong = song.copy(
                name = song.name,
                artist = song.artist,
                artwork = artworkPath,
                filePath = audioPath.toString(),
                duration = duration
            )
            songDao.updateSong(updatedSong)
            _isUpdateSuccessful.value = true
            loadSongDetails(song.id)
        }
    }

    private fun isAudioFile(uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        return mimeType?.startsWith("audio/") == true
    }
    fun resetUpdateSuccessful() {
        _isUpdateSuccessful.value = false
    }


}