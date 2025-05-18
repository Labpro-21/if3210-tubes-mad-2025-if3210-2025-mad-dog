package com.example.purrytify.ui.screens.songdetail

import android.app.Application
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.purrytify.MainViewModel
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.data.repository.OnlineSongRepository
import com.example.purrytify.db.AppDatabase
import com.example.purrytify.db.entity.RecentlyPlayed
import com.example.purrytify.db.entity.Songs
import com.example.purrytify.data.model.OnlineSongResponse
import com.example.purrytify.data.repository.ListeningActivityRepository
import com.example.purrytify.data.repository.RecommendationRepository
import com.example.purrytify.db.entity.ListeningActivity
import com.example.purrytify.utils.DownloadUtils
import com.example.purrytify.utils.MediaUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.Locale

class SongDetailViewModel(
    application: Application,
    private val mainViewModel: MainViewModel? = null
) : AndroidViewModel(application) {

    private val songDao = AppDatabase.getDatabase(application).songsDao()
    private val context = application.applicationContext
    private val recentlyPlayedDao = AppDatabase.getDatabase(application).recentlyPlayedDao()
    private val listenActivityDao = AppDatabase.getDatabase(application).listeningCapsuleDao()
    private val listenActivityRepository = ListeningActivityRepository.getInstance(listenActivityDao)
    private val authRepository = AuthRepository.getInstance(application)
    private val onlineSongRepository = OnlineSongRepository.getInstance(application)
    private val RecommendationRepository = RecommendationRepository(songDao, context, onlineSongRepository)
    private val _isUpdateSuccessful = MutableStateFlow(false)
    val isUpdateSuccessful: StateFlow<Boolean> = _isUpdateSuccessful
    private val _songDetails = MutableStateFlow<SongDetailUiState>(SongDetailUiState.Loading)
    val songDetails: StateFlow<SongDetailUiState> = _songDetails
    private val _userSongIds = MutableStateFlow<List<Int>>(emptyList())
    private val _onlineSongs = MutableStateFlow<List<OnlineSongResponse>>(emptyList())
    private val _isOnline = MutableStateFlow<Boolean>(false)
    private val tag = "SongDetailViewModel"
    private val _currentOnlineSongId = MutableStateFlow<Int?>(null)
    private val _currentOnlineRegion = MutableStateFlow<String>("GLOBAL")
    private val _isAlreadyDownloaded = MutableStateFlow(false)
    val isAlreadyDownloaded: StateFlow<Boolean> = _isAlreadyDownloaded
    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading
    private val _currentListeningActivityId = MutableStateFlow<Long?>(null) // To track the current listening activity

    private fun setCurrentOnlineSongId(songId: Int?) {
        _currentOnlineSongId.value = songId
        Log.d(tag, "Current online song ID updated to: $songId")
        songId?.let { checkIfAlreadyDownloaded(it) }
    }

    private fun setCurrentOnlineRegion(region: String) {
        _currentOnlineRegion.value = region
        Log.d(tag, "Current online region updated to: $region")
    }

    private fun getCurrentOnlineSong(): OnlineSongResponse? {
        val currentId = _currentOnlineSongId.value
        return if (currentId != null) {
            _onlineSongs.value.find { it.id == currentId }
        } else {
            Log.w(tag, "getCurrentOnlineSong: No current online song ID set.")
            null
        }
    }

    private fun loadUserSongIds() {
        viewModelScope.launch {
            authRepository.currentUserId?.let { userId ->
                songDao.getAllSongsForUser(userId).collect { songsList ->
                    _userSongIds.value = songsList.map { it.id }
                    Log.d(tag, "Loaded user song IDs: ${_userSongIds.value}")

                    // Check if current online song is downloaded whenever user songs list changes
                    _currentOnlineSongId.value?.let { checkIfAlreadyDownloaded(it) }
                }
            } ?: run {
                Log.e(tag, "User ID is null, cannot load song IDs")
            }
        }
    }

    // New function to check if a song is already downloaded
    private fun checkIfAlreadyDownloaded(songId: Int) {
        viewModelScope.launch {
            val isDownloaded = _userSongIds.value.contains(songId)
            _isAlreadyDownloaded.value = isDownloaded
            Log.d(tag, "Song ID $songId is already downloaded: $isDownloaded")
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
    fun insertListeningActivity(song: Songs, isOnline: Boolean = false) {
        Log.d(tag,"InsertListeningActivity: isOnline: $isOnline")
        if (!isOnline) {
            viewModelScope.launch {
                val userId = authRepository.currentUserId

                if (userId != null) {
                    try {
                        val listeningActivity = ListeningActivity(
                            userId = userId,
                            songId = song.id,
                            startTime = Date(),
                            endTime = null // Set to null initially as the song is still playing
                        )
                        val insertedId = listenActivityRepository.insert(listeningActivity)
                        _currentListeningActivityId.value = insertedId // Track the ID of the current listening activity
                        Log.d(tag, "Listening activity started: $listeningActivity, ID: $insertedId")
                    } catch (e: Exception) {
                        Log.e(tag, "Error inserting listening activity: ${e.message}")
                        Toast.makeText(context, "Error saving listening activity", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e(tag, "User ID is null, cannot insert listening activity")
                    Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    fun updateCompletedAndDuration() {
        viewModelScope.launch {
            try {
                val existingActivity = listenActivityDao.getById(_currentListeningActivityId.value!!.toInt()) // Assuming ID is Int in DAO
                existingActivity?.let {
                    val endTime = Date()
                    val actualDuration = endTime.time - it.startTime.time // Calculate actual duration

                    val updatedActivity = it.copy(
                        endTime = endTime,
                        duration = actualDuration,
                        completed = true
                    )
                    listenActivityRepository.update(updatedActivity)
                    Log.d(tag, "Listening activity completed. ID: ${_currentListeningActivityId.value}, Duration: $actualDuration ms")
                    _currentListeningActivityId.value = null // Reset the current listening activity ID
                } ?: run {
                    Log.w(tag, "Listening activity with ID ${_currentListeningActivityId.value} not found for update.")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error updating listening activity: ${e.message}")
                Toast.makeText(context, "Error updating listening history", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun loadSongDetails(songId: Int, isOnline: Boolean = false, region: String = "GLOBAL", isDailyPlaylist: Boolean = false) {
        viewModelScope.launch {
            _songDetails.value = SongDetailUiState.Loading // Set loading state

            try {
                if (isOnline) {
                    if (isDailyPlaylist) {
                        val song = songDao.getSongById(songId)
                        if (song != null) {
                            _songDetails.value = SongDetailUiState.Success(song)
                            return@launch
                        }
                        
                        // Not Found in db
                        val userId = authRepository.currentUserId ?: return@launch
                        val recommendationRepository = RecommendationRepository(
                            songDao,
                            context,
                            onlineSongRepository
                        )
                        
                        val dailyPlaylist = recommendationRepository.getDailyPlaylist(userId)
                        val playlistSong = dailyPlaylist.find { it.id == songId }
                        
                        if (playlistSong != null) {
                            _songDetails.value = SongDetailUiState.Success(playlistSong)
                        } else {
                            _songDetails.value = SongDetailUiState.Error("Song not found in daily playlist")
                        }
                    } else {
                        // Regular online song handling
                        var loadedSuccessfully = false
                        if (_onlineSongs.value.isEmpty()) {
                            loadedSuccessfully = loadOnlineSongsSync(region)
                        } else {
                            loadedSuccessfully = true
                        }

                        if (!loadedSuccessfully) {
                            _songDetails.value = SongDetailUiState.Error("Failed to load online songs")
                            return@launch
                        }

                        val onlineSong = _onlineSongs.value.find { it.id == songId }
                        setCurrentOnlineSongId(songId)
                        setCurrentOnlineRegion(region)

                        if (onlineSong != null) {
                            // Convert to Songs entity
                            val song = Songs(
                                id = onlineSong.id,
                                name = onlineSong.title,
                                artist = onlineSong.artist,
                                artwork = onlineSong.artwork,
                                description = onlineSong.country,
                                filePath = onlineSong.url,
                                duration = MediaUtils.parseDuration(onlineSong.duration),
                                isFavorite = false,
                                userId = authRepository.currentUserId ?: 0,
                                uploadDate = Date()
                            )
                            _songDetails.value = SongDetailUiState.Success(song)
                            checkIfAlreadyDownloaded(songId)
                        } else {
                            _songDetails.value = SongDetailUiState.Error("Online song not found")
                        }
                    }
                } else {
                    // Regular local database fetch
                    loadUserSongIds()
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

    fun insertRecentlyPlayed(song: Songs, isOnline: Boolean = false) {
        if (!isOnline) {
            viewModelScope.launch {
                val userId = authRepository.currentUserId

                if (userId != null) {
                    try {
                        val recentlyPlayed = RecentlyPlayed(
                            userId = userId,
                            songId = song.id,
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

    // Modify the skipNext function to use cached sequences if available and match MainViewModel's expected signature
    fun skipNext(currentSongId: Int, isOnline: Boolean, currentRegion: String = "GLOBAL", onNavigate: (Int) -> Unit, isDailyPlaylist: Boolean = false) {
        viewModelScope.launch {
            if (isOnline) {
                // Try to get cached song sequence from injected MainViewModel
                val cachedSongs = mainViewModel?.getOnlineSongSequence(currentRegion)
                
                if (!cachedSongs.isNullOrEmpty()) {
                    // Use cached sequence if available
                    Log.d(tag, "Using cached song sequence for region: $currentRegion")
                    val currentIndex = cachedSongs.indexOf(currentSongId)
                    if (currentIndex != -1 && currentIndex < cachedSongs.size - 1) {
                        val nextSongId = cachedSongs[currentIndex + 1]
                        onNavigate(nextSongId)
                    } else {
                        // Loop back to beginning
                        if (cachedSongs.isNotEmpty()) {
                            val firstSongId = cachedSongs.first()
                            onNavigate(firstSongId)
                        }
                    }
                    return@launch
                }
                
                // Fall back to original implementation if cache not available
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

    // Modify the skipPrevious function to use cached sequences if available
    fun skipPrevious(currentSongId: Int, isOnline: Boolean, currentRegion: String = "GLOBAL", onNavigate: (Int) -> Unit) {
        viewModelScope.launch {
            if (isOnline) {
                // Try to get cached song sequence from injected MainViewModel
                val cachedSongs = mainViewModel?.getOnlineSongSequence(currentRegion)
                
                if (!cachedSongs.isNullOrEmpty()) {
                    // Use cached sequence if available
                    Log.d(tag, "Using cached song sequence for region: $currentRegion")
                    val currentIndex = cachedSongs.indexOf(currentSongId)
                    if (currentIndex > 0) {
                        val previousSongId = cachedSongs[currentIndex - 1]
                        onNavigate(previousSongId)
                    } else {
                        // Loop back to end
                        if (cachedSongs.isNotEmpty()) {
                            val lastSongId = cachedSongs.last()
                            onNavigate(lastSongId)
                        }
                    }
                    return@launch
                }
                
                // Fall back to original implementation if cache not available
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
                filePath = audioPath!!,
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

    fun downloadSingleSong() {
        val song = getCurrentOnlineSong() ?: return

        // Skip if already downloaded
        if (_isAlreadyDownloaded.value) {
            Toast.makeText(context, "${song.title} already downloaded", Toast.LENGTH_SHORT).show()
            return
        }

        // Set downloading state
        _isDownloading.value = true
        Toast.makeText(context, "Downloading ${song.title}", Toast.LENGTH_SHORT).show()

        viewModelScope.launch {
            try {
                downloadSingleSongInternal()
                // After download completes, update downloaded state
                _isDownloading.value = false
                _isAlreadyDownloaded.value = true
                Toast.makeText(context, "Downloaded ${song.title} successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                _isDownloading.value = false
                Log.e(TAG, "Error downloading song: ${e.message}", e)
                Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun downloadSingleSongInternal() {
        val song = getCurrentOnlineSong()
        Log.d(TAG, "Attempting to download song: ${song?.title} by ${song?.artist}")
        val userId = authRepository.currentUserId!!
        Log.d(TAG, "Current user ID: $userId")
        val success = DownloadUtils.downloadAndInsertSingleSong(
            context,
            onlineSong = song!!,
            userId = userId,
            songsDao = songDao
        )
        if (success) {
            Log.d(TAG, "Successfully downloaded and inserted song: ${song.title}")
        } else {
            Log.e(TAG, "Failed to download or insert song: ${song.title}")
            throw Exception("Failed to download song")
        }
    }

    fun setOnline(online: Boolean) {
        _isOnline.value = online
    }

    companion object {
        private const val TAG = "DownloadSingleSong"
    }

    sealed class SongDetailUiState {
        object Loading : SongDetailUiState()
        data class Success(val song: Songs) : SongDetailUiState()
        data class Error(val message: String) : SongDetailUiState()
        object Empty : SongDetailUiState()
    }

    // Add a factory that can pass MainViewModel to SongDetailViewModel
    class Factory(
        private val application: Application,
        private val mainViewModel: MainViewModel
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SongDetailViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SongDetailViewModel(application, mainViewModel) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}