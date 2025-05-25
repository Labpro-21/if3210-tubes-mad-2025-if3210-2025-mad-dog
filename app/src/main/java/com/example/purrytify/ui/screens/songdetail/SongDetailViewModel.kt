package com.example.purrytify.ui.screens.songdetail

import android.app.Application
import android.content.Intent
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

    private val tag = "SongDetailViewModel"
    private val context = application.applicationContext
    private val db = AppDatabase.getDatabase(application)
    private val songDao = db.songsDao()
    private val recentlyPlayedDao = db.recentlyPlayedDao()
    private val listenActivityDao = db.listeningCapsuleDao()
    private val authRepository = AuthRepository.getInstance(application)
    private val onlineSongRepository = OnlineSongRepository.getInstance(application)
    private val listenActivityRepository = ListeningActivityRepository.getInstance(listenActivityDao)

    // UI States
    private val _songDetails = MutableStateFlow<SongDetailUiState>(SongDetailUiState.Loading)
    val songDetails: StateFlow<SongDetailUiState> = _songDetails
    private val _isUpdateSuccessful = MutableStateFlow(false)
    val isUpdateSuccessful: StateFlow<Boolean> = _isUpdateSuccessful
    private val _isAlreadyDownloaded = MutableStateFlow(false)
    val isAlreadyDownloaded: StateFlow<Boolean> = _isAlreadyDownloaded
    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading

    // Internal states
    private val _userSongIds = MutableStateFlow<List<Int>>(emptyList())
    private val _onlineSongs = MutableStateFlow<List<OnlineSongResponse>>(emptyList())
    private val _currentOnlineSongId = MutableStateFlow<Int?>(null)
    private val _currentOnlineRegion = MutableStateFlow<String>("GLOBAL")
    private val _currentListeningActivityId = MutableStateFlow<Long?>(null)

    private fun convertToSong(onlineSong: OnlineSongResponse): Songs {
        return Songs(
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
    }

    private fun setCurrentOnlineSongId(songId: Int?) {
        _currentOnlineSongId.value = songId
        songId?.let { checkIfAlreadyDownloaded(it) }
    }

    private fun setCurrentOnlineRegion(region: String) {
        _currentOnlineRegion.value = region
    }

    private fun getCurrentOnlineSong(): OnlineSongResponse? {
        val currentId = _currentOnlineSongId.value
        Log.d(tag, "Getting current online song with ID: $currentId")
        return _currentOnlineSongId.value?.let { id -> 
            _onlineSongs.value.find { it.id == id }?.also {
                Log.d(tag, "Found online song: ${it.title} by ${it.artist}")
            }
        }.also { if (it == null) Log.w(tag, "No online song found for ID: $currentId") }
    }

    private suspend fun loadOnlineSongsSync(region: String = "GLOBAL"): Boolean {
        Log.d(tag, "Loading online songs for region: $region")
        return try {
            val songs = if (region == "GLOBAL") {
                onlineSongRepository.getTopGlobalSongs()
            } else {
                onlineSongRepository.getTopCountrySongs(region.uppercase(Locale.ROOT))
            }
            
            if (songs != null) {
                _onlineSongs.value = songs
                Log.d(tag, "Successfully loaded ${songs.size} songs for region: $region")
                true
            } else {
                Log.e(tag, "Failed to load songs for region: $region - Response was null")
                false
            }
        } catch (e: Exception) {
            Log.e(tag, "Error loading online songs for region $region: ${e.message}", e)
            false
        }
    }

    private fun loadUserSongIds() {
        Log.d(tag, "Loading user song IDs")
        viewModelScope.launch {
            authRepository.currentUserId?.let { userId ->
                try {
                    // First try to get all songs immediately for navigation
                    val allSongs = songDao.getAllSongsForUserSync(userId)
                    if (allSongs.isNotEmpty()) {
                        _userSongIds.value = allSongs.map { it.id }
                        Log.d(tag, "Immediately loaded ${allSongs.size} songs for user $userId")
                    }
                    
                    // Then set up the Flow collection for updates
                    songDao.getAllSongsForUser(userId).collect { songsList ->
                        _userSongIds.value = songsList.map { it.id }
                        Log.d(tag, "Updated song list: ${songsList.size} songs for user $userId")
                        _currentOnlineSongId.value?.let { 
                            checkIfAlreadyDownloaded(it)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error loading user songs: ${e.message}", e)
                }
            } ?: Log.e(tag, "Cannot load user song IDs - User ID is null")
        }
    }

    private suspend fun ensureUserSongsLoaded(): Boolean {
        if (_userSongIds.value.isEmpty()) {
            Log.d(tag, "User songs not loaded yet, loading now")
            authRepository.currentUserId?.let { userId ->
                try {
                    val allSongs = songDao.getAllSongsForUserSync(userId)
                    _userSongIds.value = allSongs.map { it.id }
                    Log.d(tag, "Loaded ${allSongs.size} songs for user $userId")
                    return allSongs.isNotEmpty()
                } catch (e: Exception) {
                    Log.e(tag, "Error loading user songs: ${e.message}", e)
                    return false
                }
            } ?: run {
                Log.e(tag, "Cannot load user songs - User ID is null")
                return false
            }
        }
        return true
    }

    private fun checkIfAlreadyDownloaded(songId: Int) {
        val isDownloaded = _userSongIds.value.contains(songId)
        _isAlreadyDownloaded.value = isDownloaded
        Log.d(tag, "Song $songId download status: ${if (isDownloaded) "Already downloaded" else "Not downloaded"}")
    }

    fun loadSongDetails(songId: Int, isOnline: Boolean = false, region: String = "GLOBAL", isDailyPlaylist: Boolean = false) {
        Log.d(tag, "Loading song details - ID: $songId, Online: $isOnline, Region: $region, Daily Playlist: $isDailyPlaylist")
        viewModelScope.launch {
            _songDetails.value = SongDetailUiState.Loading
            try {
                if (!isOnline) {
                    loadUserSongIds()
                    songDao.getSongById(songId)?.let {
                        Log.d(tag, "Found local song: ${it.name} by ${it.artist}")
                        _songDetails.value = SongDetailUiState.Success(it)
                    } ?: run {
                        Log.e(tag, "Local song not found with ID: $songId")
                        _songDetails.value = SongDetailUiState.Error("Song not found")
                    }
                    return@launch
                }

                if (isDailyPlaylist) {
                    Log.d(tag, "Handling daily playlist song with ID: $songId")
                    handleDailyPlaylistSong(songId)
                    return@launch
                }

                // Regular online song handling
                if (_onlineSongs.value.isEmpty()) {
                    Log.d(tag, "No cached online songs, loading from network")
                    if (!loadOnlineSongsSync(region)) {
                        Log.e(tag, "Failed to load online songs for region: $region")
                        _songDetails.value = SongDetailUiState.Error("Failed to load online songs")
                        return@launch
                    }
                } else {
                    Log.d(tag, "Using cached online songs (${_onlineSongs.value.size} songs)")
                }

                val onlineSong = _onlineSongs.value.find { it.id == songId }
                setCurrentOnlineSongId(songId)
                setCurrentOnlineRegion(region)

                if (onlineSong != null) {
                    Log.d(tag, "Found online song: ${onlineSong.title} by ${onlineSong.artist}")
                    _songDetails.value = SongDetailUiState.Success(convertToSong(onlineSong))
                } else {
                    Log.e(tag, "Online song not found with ID: $songId")
                    _songDetails.value = SongDetailUiState.Error("Online song not found")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error loading song details for ID $songId: ${e.message}", e)
                _songDetails.value = SongDetailUiState.Error("Error loading song details: ${e.message}")
            }
        }
    }

    private suspend fun handleDailyPlaylistSong(songId: Int) {
        // First check local database
        songDao.getSongById(songId)?.let {
            _songDetails.value = SongDetailUiState.Success(it)
            return
        }

        // Not found in db, check daily playlist
        val userId = authRepository.currentUserId ?: run {
            _songDetails.value = SongDetailUiState.Error("User not logged in")
            return
        }

        val recommendationRepository = RecommendationRepository(songDao, context, onlineSongRepository)
        val dailyPlaylist = recommendationRepository.getDailyPlaylist(userId)
        
        dailyPlaylist.find { it.id == songId }?.let {
            _songDetails.value = SongDetailUiState.Success(it)
        } ?: run {
            _songDetails.value = SongDetailUiState.Error("Song not found in daily playlist")
        }
    }

    fun fetchSongByDeepLink(songId: Int) {
        viewModelScope.launch {
            _songDetails.value = SongDetailUiState.Loading
            try {
                if (!loadOnlineSongsSync("GLOBAL")) {
                    _songDetails.value = SongDetailUiState.Error("Failed to load online songs")
                    return@launch
                }

                var onlineSong = _onlineSongs.value.find { it.id == songId }
                    ?: onlineSongRepository.getSongById(songId)

                if (onlineSong != null) {
                    setCurrentOnlineSongId(songId)
                    setCurrentOnlineRegion("GLOBAL")
                    _songDetails.value = SongDetailUiState.Success(convertToSong(onlineSong))
                } else {
                    _songDetails.value = SongDetailUiState.Error("Failed to fetch song from server")
                }
            } catch (e: Exception) {
                _songDetails.value = SongDetailUiState.Error("Error fetching song: ${e.message}")
            }
        }
    }

    fun downloadSingleSong() {
        Log.d(tag, "Attempting to download song - Current online song ID: ${_currentOnlineSongId.value}, Region: ${_currentOnlineRegion.value}")
        
        // First ensure we have online songs loaded
        viewModelScope.launch {
            try {
                // Load online songs if not already loaded
                if (_onlineSongs.value.isEmpty()) {
                    Log.d(tag, "No online songs loaded, loading from network for region: ${_currentOnlineRegion.value}")
                    if (!loadOnlineSongsSync(_currentOnlineRegion.value)) {
                        Log.e(tag, "Failed to load online songs")
                        Toast.makeText(context, "Failed to load song data", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                }

                // Try to get the song from loaded songs
                val song = _onlineSongs.value.find { it.id == _currentOnlineSongId.value } ?: run {
                    Log.e(tag, "Song not found in loaded songs, trying direct API call")
                    // If not found in loaded songs, try direct API call
                    val directSong = onlineSongRepository.getSongById(_currentOnlineSongId.value ?: return@launch)
                    if (directSong == null) {
                        Log.e(tag, "Song not found via direct API call")
                        Toast.makeText(context, "Song not found", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    directSong
                }

                Log.d(tag, "Found song to download: ${song.title} by ${song.artist}")

                if (_isAlreadyDownloaded.value) {
                    Log.d(tag, "Song already downloaded: ${song.title}")
                    Toast.makeText(context, "${song.title} already downloaded", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                Log.d(tag, "Starting download for song: ${song.title} by ${song.artist}")
                _isDownloading.value = true
                Toast.makeText(context, "Downloading ${song.title}", Toast.LENGTH_SHORT).show()

                try {
                    val success = DownloadUtils.downloadAndInsertSingleSong(
                        context,
                        song,
                        authRepository.currentUserId!!,
                        songDao
                    )
                    
                    _isDownloading.value = false
                    if (success) {
                        Log.d(tag, "Successfully downloaded song: ${song.title}")
                        _isAlreadyDownloaded.value = true
                        Toast.makeText(context, "Downloaded ${song.title} successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e(tag, "Failed to download song: ${song.title}")
                        throw Exception("Failed to download song")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error downloading song ${song.title}: ${e.message}", e)
                    _isDownloading.value = false
                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(tag, "Error in downloadSingleSong: ${e.message}", e)
                _isDownloading.value = false
                Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun insertRecentlyPlayed(song: Songs, isOnline: Boolean = false) {
        if (isOnline) return

        viewModelScope.launch {
            try {
                val userId = authRepository.currentUserId ?: return@launch
                val recentlyPlayed = RecentlyPlayed(
                    userId = userId,
                    songId = song.id,
                    playedAt = Date()
                )
                recentlyPlayedDao.insertRecentlyPlayed(recentlyPlayed)
            } catch (e: Exception) {
                Log.e(tag, "Error inserting recently played: ${e.message}")
                Toast.makeText(context, "Error saving recently played", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun skipNext(currentSongId: Int, isOnline: Boolean, currentRegion: String = "GLOBAL", onNavigate: (Int) -> Unit, isDailyPlaylist: Boolean = false) {
        Log.d(tag, "Skip Next - Current ID: $currentSongId, Online: $isOnline, Region: $currentRegion, Daily Playlist: $isDailyPlaylist")
        viewModelScope.launch {
            if (!isOnline) {
                Log.d(tag, "Handling local skip next")
                if (ensureUserSongsLoaded()) {
                    handleLocalSkipNext(currentSongId, onNavigate)
                } else {
                    Log.e(tag, "Failed to load user songs for navigation")
                }
                return@launch
            }

            // Try cached sequence first
            mainViewModel?.getOnlineSongSequence(currentRegion)?.let { cachedSongs ->
                if (cachedSongs.isNotEmpty()) {
                    Log.d(tag, "Using cached song sequence (${cachedSongs.size} songs)")
                    handleCachedSkipNext(cachedSongs, currentSongId, onNavigate)
                    return@launch
                }
            }

            Log.d(tag, "No cached sequence available, falling back to online songs")
            // Fallback to online songs
            if (_onlineSongs.value.isEmpty()) {
                Log.d(tag, "Loading online songs for region: $currentRegion")
                if (!loadOnlineSongsSync(currentRegion)) {
                    Log.e(tag, "Failed to load online songs for region: $currentRegion")
                    return@launch
                }
            }

            handleOnlineSkipNext(currentSongId, onNavigate)
        }
    }

    private fun handleLocalSkipNext(currentSongId: Int, onNavigate: (Int) -> Unit) {
        val currentList = _userSongIds.value
        val currentIndex = currentList.indexOf(currentSongId)
        Log.d(tag, "Local skip next - Current index: $currentIndex, Total songs: ${currentList.size}")
        
        if (currentIndex != -1 && currentIndex < currentList.size - 1) {
            val nextId = currentList[currentIndex + 1]
            Log.d(tag, "Moving to next local song: $nextId")
            onNavigate(nextId)
        } else if (currentList.isNotEmpty()) {
            val firstId = currentList.first()
            Log.d(tag, "Wrapping to first local song: $firstId")
            onNavigate(firstId)
        }
    }

    private fun handleCachedSkipNext(cachedSongs: List<Int>, currentSongId: Int, onNavigate: (Int) -> Unit) {
        val currentIndex = cachedSongs.indexOf(currentSongId)
        Log.d(tag, "Cached skip next - Current index: $currentIndex, Total songs: ${cachedSongs.size}")
        
        if (currentIndex != -1 && currentIndex < cachedSongs.size - 1) {
            val nextId = cachedSongs[currentIndex + 1]
            Log.d(tag, "Moving to next cached song: $nextId")
            onNavigate(nextId)
        } else if (cachedSongs.isNotEmpty()) {
            val firstId = cachedSongs.first()
            Log.d(tag, "Wrapping to first cached song: $firstId")
            onNavigate(firstId)
        }
    }

    private fun handleOnlineSkipNext(currentSongId: Int, onNavigate: (Int) -> Unit) {
        val currentIndex = _onlineSongs.value.indexOfFirst { it.id == currentSongId }
        Log.d(tag, "Online skip next - Current index: $currentIndex, Total songs: ${_onlineSongs.value.size}")
        
        if (currentIndex != -1 && currentIndex < _onlineSongs.value.size - 1) {
            val nextSong = _onlineSongs.value[currentIndex + 1]
            Log.d(tag, "Moving to next online song: ${nextSong.id} (${nextSong.title})")
            onNavigate(nextSong.id)
        } else if (_onlineSongs.value.isNotEmpty()) {
            val firstSong = _onlineSongs.value.first()
            Log.d(tag, "Wrapping to first online song: ${firstSong.id} (${firstSong.title})")
            onNavigate(firstSong.id)
        }
    }

    fun skipPrevious(currentSongId: Int, isOnline: Boolean, currentRegion: String = "GLOBAL", onNavigate: (Int) -> Unit) {
        viewModelScope.launch {
            if (!isOnline) {
                if (ensureUserSongsLoaded()) {
                    handleLocalSkipPrevious(currentSongId, onNavigate)
                } else {
                    Log.e(tag, "Failed to load user songs for navigation")
                }
                return@launch
            }

            // Try cached sequence first
            mainViewModel?.getOnlineSongSequence(currentRegion)?.let { cachedSongs ->
                if (cachedSongs.isNotEmpty()) {
                    handleCachedSkipPrevious(cachedSongs, currentSongId, onNavigate)
                    return@launch
                }
            }

            // Fallback to online songs
            if (_onlineSongs.value.isEmpty() && !loadOnlineSongsSync(currentRegion)) {
                return@launch
            }

            handleOnlineSkipPrevious(currentSongId, onNavigate)
        }
    }

    private fun handleLocalSkipPrevious(currentSongId: Int, onNavigate: (Int) -> Unit) {
        val currentList = _userSongIds.value
        val currentIndex = currentList.indexOf(currentSongId)
        
        if (currentIndex > 0) {
            onNavigate(currentList[currentIndex - 1])
        } else if (currentList.isNotEmpty()) {
            onNavigate(currentList.last())
        }
    }

    private fun handleCachedSkipPrevious(cachedSongs: List<Int>, currentSongId: Int, onNavigate: (Int) -> Unit) {
        val currentIndex = cachedSongs.indexOf(currentSongId)
        if (currentIndex > 0) {
            onNavigate(cachedSongs[currentIndex - 1])
        } else if (cachedSongs.isNotEmpty()) {
            onNavigate(cachedSongs.last())
        }
    }

    private fun handleOnlineSkipPrevious(currentSongId: Int, onNavigate: (Int) -> Unit) {
        val currentIndex = _onlineSongs.value.indexOfFirst { it.id == currentSongId }
        if (currentIndex > 0) {
            onNavigate(_onlineSongs.value[currentIndex - 1].id)
        } else if (_onlineSongs.value.isNotEmpty()) {
            onNavigate(_onlineSongs.value.last().id)
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
            val artworkPath = photoUri?.toString() ?: song.artwork
            var audioPath = song.filePath
            var duration = song.duration

            if (fileUri != null && isAudioFile(fileUri)) {
                audioPath = fileUri.toString()
                duration = MediaUtils.getAudioDuration(fileUri, context)
            }

            val updatedSong = song.copy(
                name = song.name,
                artist = song.artist,
                artwork = artworkPath,
                filePath = audioPath,
                duration = duration
            )
            songDao.updateSong(updatedSong)
            _isUpdateSuccessful.value = true
            loadSongDetails(song.id)
        }
    }

    private fun isAudioFile(uri: Uri): Boolean {
        return context.contentResolver.getType(uri)?.startsWith("audio/") == true
    }

    fun resetUpdateSuccessful() {
        _isUpdateSuccessful.value = false
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

    sealed class SongDetailUiState {
        object Loading : SongDetailUiState()
        data class Success(val song: Songs) : SongDetailUiState()
        data class Error(val message: String) : SongDetailUiState()
        object Empty : SongDetailUiState()
    }
}