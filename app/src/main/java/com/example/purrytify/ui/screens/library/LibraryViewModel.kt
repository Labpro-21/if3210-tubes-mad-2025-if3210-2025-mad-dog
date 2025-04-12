package com.example.purrytify.ui.screens.library

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import com.example.purrytify.utils.MediaUtils
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.db.AppDatabase
import com.example.purrytify.db.entity.Songs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val songDao = AppDatabase.getDatabase(application).songsDao()
    private val context = application.applicationContext
    private val authRepository = AuthRepository.getInstance(application)


    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val userId: Int?
        get() = authRepository.currentUserId

    val allSongs: Flow<List<Songs>>
        get() {
            val userId = this.userId
            return if (userId != null) {
                songDao.getAllSongsForUser(userId)
            } else {
                songDao.getAllSongs()
            }
        }

    val favoriteSongs: Flow<List<Songs>>
        get() {
            val userId = this.userId
            return if (userId != null) {
                songDao.getFavoriteSongsForUser(userId)
            } else {
                songDao.getFavoriteSongs()
            }
        }

    val searchResults: Flow<List<Songs>>
    get() {
        val userId = this.userId
        val query = _searchQuery.value
        
        if (userId == null) {
            return kotlinx.coroutines.flow.flowOf(emptyList())
        }
        
        return if (query.isEmpty()) {
            songDao.getAllSongsForUser(userId)
        } else {
            songDao.searchSongsForUser(userId, query)
        }
    }
        
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addSong(uri: Uri?, artworkUri: Uri?, title: String, artist: String) {
        val userId = authRepository.currentUserId

        if (userId == null) {
            Toast.makeText(context, "You must be logged in to add songs.", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            if (uri != null && isAudioFile(uri)) {
                val duration = MediaUtils.getAudioDuration(uri, context)
                val artworkPath = artworkUri?.toString() ?: ""

                val newSong = Songs(
                    artist = artist,
                    name = title,
                    description = "",
                    userId = userId,
                    duration = duration,
                    filePath = uri.toString(),
                    uploadDate = Date(),
                    artwork = artworkPath
                )
                songDao.insertSong(newSong)
            } else {
                Toast.makeText(context, "Please select a valid audio file.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isAudioFile(uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        return mimeType?.startsWith("audio/") == true
    }
}