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
import com.example.purrytify.db.AppDatabase
import com.example.purrytify.db.entity.Songs
import com.example.purrytify.utils.MediaUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class SongDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val songDao = AppDatabase.getDatabase(application).songsDao()
    private val context = application.applicationContext

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
    fun getAudioDuration(uri: Uri, context: Context): Long {
        val retriever = MediaMetadataRetriever()
        try {
            if (uri.scheme == "content") {
                retriever.setDataSource(context, uri)
            } else if (uri.scheme == "file") {
                retriever.setDataSource(uri.path)
            }

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            Log.d("Metadata", "Duration: $durationStr  URI: $uri")

            return durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            return 0L
        } finally {
            retriever.release()
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