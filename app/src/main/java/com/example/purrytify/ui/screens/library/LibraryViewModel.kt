package com.example.purrytify.ui.screens.library

import android.app.Application
import android.media.MediaMetadataRetriever
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.db.AppDatabase
import com.example.purrytify.db.entity.Songs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val songDao = AppDatabase.getDatabase(application).songsDao()

    val allSongs: Flow<List<Songs>> = songDao.getAllSongs()
    val favoriteSongs: Flow<List<Songs>> = songDao.getFavoriteSongs()

    fun addSong(song: Songs) {
        viewModelScope.launch {
            songDao.insert(song)
        }
    }

    fun toggleFavoriteStatus(song: Songs) {
        viewModelScope.launch {
            songDao.updateFavoriteStatus(song.id, !song.isFavorite)
        }
    }

    fun deleteSong(song: Songs) {
        viewModelScope.launch {
            songDao.deleteById(song.id)
        }
    }
}
