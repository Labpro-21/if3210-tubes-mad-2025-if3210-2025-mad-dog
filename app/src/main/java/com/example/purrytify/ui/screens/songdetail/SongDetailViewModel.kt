package com.example.purrytify.ui.screens.songdetail

import android.app.Application
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.data.auth.OnlineSongRepository
import com.example.purrytify.db.AppDatabase
import com.example.purrytify.db.entity.RecentlyPlayed
import com.example.purrytify.db.entity.Songs
import com.example.purrytify.data.model.OnlineSongResponse
import com.example.purrytify.utils.MediaUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SongDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val songDao = AppDatabase.getDatabase(application).songsDao()
    private val context = application.applicationContext
    private val recentlyPlayedDao = AppDatabase.getDatabase(application).recentlyPlayedDao()
    private val authRepository = AuthRepository.getInstance(application)
    private val onlineSongRepository = OnlineSongRepository.getInstance(application)

    private val _isUpdateSuccessful = MutableStateFlow(false)
    val isUpdateSuccessful: StateFlow<Boolean> = _isUpdateSuccessful

    private val _songDetails = MutableStateFlow<SongDetailUiState>(SongDetailUiState.Loading)
    val songDetails: StateFlow<SongDetailUiState> = _songDetails

    private val _userSongIds = MutableStateFlow<List<Int>>(emptyList())
    val userSongIds: StateFlow<List<Int>> = _userSongIds

    private val _onlineSongs = MutableStateFlow<List<OnlineSongResponse>>(emptyList())
    val onlineSongs: StateFlow<List<OnlineSongResponse>> = _onlineSongs
    private val tag = "SongDetailViewModel"

    init {
        loadUserSongIds()
    }

    private fun loadUserSongIds() {
        viewModelScope.launch {
            authRepository.currentUserId?.let { userId ->
                songDao.getAllSongsForUser(userId).collect { songsList ->
                    _userSongIds.value = songsList.map { it.id }
                    Log.d(tag, "Loaded user song IDs: ${_userSongIds.value}")
                }
            } ?: run {
                Log.e(tag, "User ID is null, cannot load song IDs")
            }
        }
    }

    private suspend fun loadOnlineSongsSync(region: String = "GLOBAL"): Boolean {
        return try {
            Log.d(tag,"loadOnlineSongsSync Region: $region")
            withContext(Dispatchers.IO) {
                val onlinesongs: List<OnlineSongResponse>? = if (region == "GLOBAL") {
                    onlineSongRepository.getTopGlobalSongs()
                } else {
                    onlineSongRepository.getTopCountrySongs(region.uppercase(Locale.ROOT))
                }

                if (onlinesongs != null) {
                    _onlineSongs.value = onlinesongs
                    Log.d(tag, "Loaded online Songs: ${_onlineSongs.value}")
                    true
                } else {
                    Log.e(tag, "Failed to load online songs: response was null")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to load online songs: ${e.message}", e)
            false
        }
    }

    fun loadOnlineSongs() {
        viewModelScope.launch {
            loadOnlineSongsSync()
        }
    }

    fun loadSongDetails(songId: Int, isOnline: Boolean = false, region: String = "GLOBAL") {
        viewModelScope.launch {
            _songDetails.value = SongDetailUiState.Loading // Set loading state

            try {
                if (isOnline) {
                    // Ensure online songs are loaded and wait for completion
                    var loadedSuccessfully = false
                    if (_onlineSongs.value.isEmpty()) {
                        Log.d(tag, "Online songs empty, loading now...")
                        loadedSuccessfully = loadOnlineSongsSync(region)
                        Log.d(tag, "Online songs loaded successfully: $loadedSuccessfully")
                    } else {
                        loadedSuccessfully = true
                    }

                    if (!loadedSuccessfully) {
                        _songDetails.value = SongDetailUiState.Error("Failed to load online songs")
                        return@launch
                    }

                    val onlineSong = _onlineSongs.value.find { it.id == songId }
                    Log.d(tag, "Album Region: ${region}")
                    Log.d(tag, "Online songs count: ${_onlineSongs.value.size}")
                    Log.d(tag, "Looking for song ID: $songId")
                    Log.d(tag, "Found online song: $onlineSong")

                    if (onlineSong != null) {
                        // Map OnlineSongResponse to Songs (local database entity)
                        val song = Songs(
                            id = onlineSong.id,
                            name = onlineSong.title,
                            artist = onlineSong.artist,
                            artwork = onlineSong.artwork,
                            description = "",
                            filePath = onlineSong.url,
                            duration = parseDuration(onlineSong.duration),
                            isFavorite = false,
                            userId = authRepository.currentUserId ?: 0,
                            uploadDate = Date()
                        )
                        _songDetails.value = SongDetailUiState.Success(song)
                    } else {
                        _songDetails.value = SongDetailUiState.Error("Online song not found")
                    }
                } else {
                    // Fetch from local database
                    val song = songDao.getSongById(songId)
                    if (song != null) {
                        _songDetails.value = SongDetailUiState.Success(song)
                    } else {
                        _songDetails.value = SongDetailUiState.Error("Song not found")
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error loading song details", e)
                _songDetails.value = SongDetailUiState.Error("Error loading song details: ${e.message}")
            }
        }
    }

    fun insertRecentlyPlayed(songId: Int, isOnline: Boolean = false) {
        if (!isOnline) {
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
                        Log.e(tag, "Error inserting recently played: ${e.message}")
                        Toast.makeText(context, "Error saving recently played", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e(tag, "User ID is null, cannot insert recently played")
                    Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun skipNext(currentSongId: Int, isOnline: Boolean, currentRegion: String = "GLOBAL", onNavigate: (Int) -> Unit) {
        viewModelScope.launch {
            if (isOnline) {
                if (_onlineSongs.value.isEmpty()) {
                    val loaded = loadOnlineSongsSync(currentRegion) // Try to reload
                    if (!loaded) {
                        // Handle error: could not load songs
                        Log.d(tag, "Failed to load online songs for region: $currentRegion")
                        return@launch
                    }
                }

                Log.d(tag, "----- Online Songs for Region: $currentRegion -----")
                _onlineSongs.value.forEachIndexed { index, onlineSong ->
                    Log.d(tag, "[$index] ID: ${onlineSong.id}, Title: ${onlineSong.title}, Artist: ${onlineSong.artist}, Artwork: ${onlineSong.artwork}, URL: ${onlineSong.url}, Duration: ${onlineSong.duration}")
                }
                Log.d(tag, "--------------------------------------------------")

                val currentOnlineSong = _onlineSongs.value.find { it.id == currentSongId }
                val currentIndex = _onlineSongs.value.indexOf(currentOnlineSong)
                Log.d(tag, "currentOnlineSong: $currentOnlineSong")
                Log.d(tag, "currentIndex: $currentIndex")

                if (currentIndex != -1 && currentIndex < _onlineSongs.value.size - 1) {
                    val nextSongId = _onlineSongs.value[currentIndex + 1].id
                    onNavigate(nextSongId)
                } else {
                    // If at the end of the list, loop back to the beginning
                    if (_onlineSongs.value.isNotEmpty()) {
                        val firstSongId = _onlineSongs.value.first().id
                        onNavigate(firstSongId)
                    }
                }
            } else {
                val currentList = _userSongIds.value
                val currentIndex = currentList.indexOf(currentSongId)

                if (currentIndex != -1 && currentIndex < currentList.size - 1) {
                    val nextSongId = currentList[currentIndex + 1]
                    Log.d("SongDetailsViewModel: ", "Next Song ID: $nextSongId")
                    onNavigate(nextSongId)
                } else {
                    // If at the end of the local list, loop back to the beginning
                    if (currentList.isNotEmpty()) {
                        val firstSongId = currentList.first()
                        Log.d("SongDetailsViewModel: ", "FirstSong ID: $firstSongId")
                        onNavigate(firstSongId)
                    }
                }
            }
        }
    }

    fun skipPrevious(currentSongId: Int, isOnline: Boolean, currentRegion: String = "GLOBAL", onNavigate: (Int) -> Unit) {
        viewModelScope.launch {
            if (isOnline) {
                if (_onlineSongs.value.isEmpty()) {
                    val loaded = loadOnlineSongsSync(currentRegion) // Try to reload
                    if (!loaded) {
                        // Handle error: could not load songs
                        Log.d(tag, "Failed to load online songs for region: $currentRegion")
                        return@launch
                    }
                }

                Log.d(tag, "----- Online Songs for Region: $currentRegion -----")
                _onlineSongs.value.forEachIndexed { index, onlineSong ->
                    Log.d(tag, "[$index] ID: ${onlineSong.id}, Title: ${onlineSong.title}, Artist: ${onlineSong.artist}, Artwork: ${onlineSong.artwork}, URL: ${onlineSong.url}, Duration: ${onlineSong.duration}")
                }
                Log.d(tag, "--------------------------------------------------")

                val currentOnlineSong = _onlineSongs.value.find { it.id == currentSongId }
                val currentIndex = _onlineSongs.value.indexOf(currentOnlineSong)
                Log.d(tag, "currentOnlineSong: $currentOnlineSong")
                Log.d(tag, "currentIndex: $currentIndex")

                if (currentIndex > 0) {
                    val previousSongId = _onlineSongs.value[currentIndex - 1].id
                    onNavigate(previousSongId)
                } else {
                    // If at the beginning of the list, loop to the end
                    if (_onlineSongs.value.isNotEmpty()) {
                        val lastSongId = _onlineSongs.value.last().id
                        onNavigate(lastSongId)
                    }
                }
            } else {
                val currentList = _userSongIds.value
                val currentIndex = currentList.indexOf(currentSongId)

                if (currentIndex > 0) {
                    val previousSongId = currentList[currentIndex - 1]
                    onNavigate(previousSongId)
                } else {
                    // If at the beginning of the local list, loop to the end
                    if (currentList.isNotEmpty()) {
                        val lastSongId = currentList.last()
                        onNavigate(lastSongId)
                    }
                }
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
                artworkPath = photoUri.toString()
            }

            if (fileUri != null && isAudioFile(fileUri)) {
                audioPath = fileUri.toString()
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

    private fun parseDuration(durationString: String): Long {
        return try {
            val parts = durationString.split(":")

            when (parts.size) {
                2 -> {
                    val minutes = parts[0].toLong()
                    val seconds = parts[1].toLong()
                    (minutes * 60 + seconds) * 1000
                }

                3 -> {
                    val hours = parts[0].toLong()
                    val minutes = parts[1].toLong()
                    val seconds = parts[2].toLong()
                    ((hours * 60 * 60) + (minutes * 60) + seconds) * 1000
                }
                else -> {
                    Log.e(tag, "Unexpected duration format: $durationString")
                    0L
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error parsing duration: ${durationString}, ${e.message}")
            0L
        }
    }

    sealed class SongDetailUiState {
        object Loading : SongDetailUiState()
        data class Success(val song: Songs) : SongDetailUiState()
        data class Error(val message: String) : SongDetailUiState()
        object Empty : SongDetailUiState()
    }
}