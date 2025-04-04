// SongDetailViewModel.kt

package com.example.purrytify.ui.screens.songdetail

import android.app.Application
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.db.AppDatabase
import com.example.purrytify.db.entity.RecentlyPlayed
import com.example.purrytify.db.entity.Songs
import com.example.purrytify.utils.MediaUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Date

class SongDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val songDao = AppDatabase.getDatabase(application).songsDao()
    private val context = application.applicationContext
    private val recentlyPlayedDao = AppDatabase.getDatabase(application).recentlyPlayedDao()
    private val authRepository = AuthRepository.getInstance(application)

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

    fun getMetadata(uri: Uri?): Pair<String?, String?> {
        if (uri == null) return Pair(null, null)

        val retriever = MediaMetadataRetriever()
        var title: String? = null
        var artist: String? = null

        try {
            if (uri.scheme == "content") {
                retriever.setDataSource(context, uri)
            } else if (uri.scheme == "file") {
                retriever.setDataSource(uri.path)
            }

            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)

        } catch (e: Exception) {
            Log.e("Player", "Error getting metadata: ${e.message}")
        } finally {
            retriever.release()
        }

        return Pair(title, artist)
    }

    fun updateSong(song: Songs, photoUri: Uri?, fileUri: Uri?) {
        viewModelScope.launch {
            var artworkFilePath: String? = song.artwork
            var audioFilePath: String? = song.filePath

            if (photoUri != null) {
                artworkFilePath = MediaUtils.copyArtworkToInternalStorage(photoUri,context)
            }

            if (fileUri != null && isAudioFile(fileUri)) {
                audioFilePath = MediaUtils.copyAudioToInternalStorage(fileUri,context)

            }
            if(audioFilePath!= null){
                val duration = MediaUtils.getAudioDuration(Uri.fromFile(File(audioFilePath)), context)
                val updatedSong = song.copy(
                    name = song.name,
                    artist = song.artist,
                    artwork = artworkFilePath,
                    filePath = audioFilePath,
                    duration =duration
                )
                songDao.updateSong(updatedSong)

            }
            loadSongDetails(song.id)
        }
    }

    private fun isAudioFile(uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        return mimeType?.startsWith("audio/") == true
    }


}