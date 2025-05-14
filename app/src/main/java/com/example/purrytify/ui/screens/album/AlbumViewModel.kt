import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.data.repository.OnlineSongRepository
import com.example.purrytify.data.model.OnlineSongResponse
import com.example.purrytify.data.repository.RecommendationRepository
import com.example.purrytify.db.AppDatabase // Assuming this is your Room database class
import com.example.purrytify.utils.DownloadUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class AlbumViewModel(application: Application) : AndroidViewModel(application) {
    private val onlineSongRepository = OnlineSongRepository.getInstance(application)
    private val songsDao = AppDatabase.getDatabase(application).songsDao() // Get the SongsDao instance
    private val tag = "AlbumViewModel"
    private val authRepository = AuthRepository.getInstance(application)
    private val context = application.applicationContext // Get application context
    private val _downloadProgress = MutableStateFlow<Pair<Int, Int>?>(null) // Pair<DownloadedCount, TotalCount>
    val downloadProgress: StateFlow<Pair<Int, Int>?> = _downloadProgress


    private val _songs = MutableStateFlow<List<OnlineSongResponse>>(emptyList())
    val songs: StateFlow<List<OnlineSongResponse>> = _songs

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _onlineSongs = MutableStateFlow<List<OnlineSongResponse>>(emptyList())
    val onlineSongs: StateFlow<List<OnlineSongResponse>> = _onlineSongs

    fun getCurrentUserid(): Int {
        return authRepository.currentUserId!!
    }

    fun loadSongs(region: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val fetchedSongs = if (region.uppercase(Locale.ROOT) == "GLOBAL") {
                    onlineSongRepository.getTopGlobalSongs()
                } else {
                    onlineSongRepository.getTopCountrySongs(region.uppercase(Locale.ROOT))
                }

                if (fetchedSongs != null) {
                    _songs.value = fetchedSongs
                    Log.d(tag, "Fetched ${fetchedSongs.size} online songs for region $region")
                } else {
                    _errorMessage.value = "Failed to fetch songs for $region"
                    Log.e(tag, "Failed to fetch songs for $region")
                }
            } catch (e: Exception) {
                _errorMessage.value = "An error occurred: ${e.localizedMessage}"
                Log.e(tag, "Error loading songs for $region: ${e.localizedMessage}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadDailyPlaylist(region: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val userId = getCurrentUserid()
                
                val recommendationRepository = RecommendationRepository(
                    songsDao,
                    getApplication(),
                    onlineSongRepository
                )

                
                val dailyPlaylistSongs = recommendationRepository.getDailyPlaylist(userId, region)

                val onlineSongs = dailyPlaylistSongs.mapIndexed { index, song ->
                    OnlineSongResponse(
                        id = song.id ?: 0,
                        title = song.name ?: "",
                        artist = song.artist ?: "",
                        duration = song.duration?.toString() ?: "0:00",
                        url = song.filePath ?: "",
                        artwork = song.artwork ?: "",
                        country = region, 
                        createdAt = "",
                        updatedAt = "",
                        rank = index + 1
                    )
                }

                _songs.value = onlineSongs
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load daily playlist: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun downloadSongs(region: String, userId: Int, isDailyPlaylist: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                // Use current songs value if it's a daily playlist, otherwise fetch from repository
                val fetchedSongs = if (isDailyPlaylist) {
                    _songs.value
                } else if (region.uppercase(Locale.ROOT) == "GLOBAL") {
                    onlineSongRepository.getTopGlobalSongs()
                } else {
                    onlineSongRepository.getTopCountrySongs(region.uppercase(Locale.ROOT))
                }
                
                if (fetchedSongs != null && fetchedSongs.isNotEmpty()) {
                    _onlineSongs.value = fetchedSongs
                    Log.d(tag, "Loaded ${if (isDailyPlaylist) "daily playlist" else "online"} songs: ${fetchedSongs.size}")
                    insertSongsIntoDb(fetchedSongs, userId)
                } else {
                    Log.e(tag, "Failed to load songs: response was null or empty")
                    _errorMessage.value = "Failed to download songs: No songs found"
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to load songs: ${e.message}", e)
                _errorMessage.value = "Failed to download songs: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }


    private suspend fun insertSongsIntoDb(onlineSongs: List<OnlineSongResponse>, userId: Int) {
        val totalSongs = onlineSongs.size
        var downloadedCount = 0

        onlineSongs.forEach { onlineSong ->
            val success = DownloadUtils.downloadAndInsertSingleSong(context, onlineSong, userId, songsDao)
            if (success) {
                downloadedCount++
            } else {
                Log.e(tag, "Failed to download and insert song: ${onlineSong.title}")
            }
            _downloadProgress.value = Pair(downloadedCount, totalSongs) // Update progress
        }
        Log.d(tag, "Attempted to download ${onlineSongs.size} songs.")
        _downloadProgress.value = null // Reset progress when done
    }

    class Factory(val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AlbumViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AlbumViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}