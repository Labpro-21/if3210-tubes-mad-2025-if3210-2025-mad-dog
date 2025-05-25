package com.example.purrytify.ui.screens.profile
import NetworkMonitor
import android.app.Application
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.data.repository.ProfileRepository
import com.example.purrytify.data.model.ProfileResponse
import com.example.purrytify.data.repository.ListeningActivityRepository
import com.example.purrytify.db.AppDatabase
import com.example.purrytify.db.dao.ListeningActivityDao
import com.example.purrytify.db.dao.SoundCapsuleDao
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "ProfileViewModel"
        
        // Refresh interval for Sound Capsule data when viewing profile screen (in milliseconds)
        const val UI_REFRESH_INTERVAL = 30000 // 30 seconds
    }
    
    private val repository = ProfileRepository.getInstance(application)
    private val networkMonitor = NetworkMonitor

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application.applicationContext)

    private val _profile = MutableStateFlow<ProfileResponse?>(null)
    val profile: StateFlow<ProfileResponse?> = _profile

    private val _songsCount = MutableStateFlow(0)
    val songsCount: StateFlow<Int> = _songsCount

    private val _playedCount = MutableStateFlow(0)
    val playedCount: StateFlow<Int> = _playedCount

    private val _favoriteCount = MutableStateFlow(0)
    val favoriteCount: StateFlow<Int> = _favoriteCount

    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _noInternet = MutableStateFlow(false)
    val noInternet: StateFlow<Boolean> = _noInternet

    private val _updateProfileStatus = MutableStateFlow<UpdateProfileStatus>(UpdateProfileStatus.Initial)
    val updateProfileStatus: StateFlow<UpdateProfileStatus> = _updateProfileStatus

    private val _currentLocation = MutableStateFlow<String?>(null)
    val currentLocation: StateFlow<String?> = _currentLocation

    private val _soundCapsuleData = MutableStateFlow<SoundCapsuleViewModel?>(null)
    val soundCapsuleData: StateFlow<SoundCapsuleViewModel?> = _soundCapsuleData

    // Add new state flow for multiple sound capsules
    private val _soundCapsulesData = MutableStateFlow<List<SoundCapsuleViewModel>>(emptyList())
    val soundCapsulesData: StateFlow<List<SoundCapsuleViewModel>> = _soundCapsulesData

    private val authRepository = AuthRepository.getInstance(application)

    private val appDatabase = AppDatabase.getDatabase(application)
    private val listenActivityDao = appDatabase.listeningCapsuleDao()
    private val soundCapsuleDao = appDatabase.soundCapsuleDao()
    private val listenActivityRepository = ListeningActivityRepository.getInstance(listenActivityDao)
    
    // Data class for UI representation of SoundCapsule
    data class SoundCapsuleViewModel(
        val totalTimeListened: Long,
        val topArtist: String?,
        val topSong: TopSongViewModel?,
        val listeningDayStreak: Int,
        val monthYear: String,
        val streakSongs: List<DayStreakSongViewModel>,
        val topStreakSong: DayStreakSongViewModel?
    ) {
        data class TopSongViewModel(
            val id: Int,
            val name: String,
            val artist: String,
            val description: String,
            val duration: Long,
            val artwork: String?
        )
        
        data class DayStreakSongViewModel(
            val songId: Int?,
            val name: String?,
            val artist: String?,
            val artwork: String?,
            val playDate: String,
            val playCount: Int,
            val dayCount: Int = 0
        )
    }
    
    fun getSoundCapsule() {
        viewModelScope.launch(Dispatchers.IO) {
            val userId = authRepository.currentUserId
            if (userId != null) {
                try {
                    // Get current date
                    val currentDate = LocalDate.now()
                    val year = currentDate.year
                    val month = currentDate.monthValue
                    
                    // First, get the current data to display immediately
                    val initialData = soundCapsuleDao.getSoundCapsuleWithDetails(userId)
                    _soundCapsuleData.value = convertToViewModel(initialData)
                    
                    // Then, set up continuous observation of the Sound Capsule data
                    // This will automatically update the UI when the database changes
                    observeSoundCapsuleData(userId, year, month)
                    
                    // Get multiple sound capsules (up to 3) for the profile display
                    getMultipleSoundCapsules(userId)
                    
                    Log.d("SoundCapsuleData", "Initial data loaded and continuous observation started")
                } catch (e: Exception) {
                    Log.e("ProfileViewModel", "Error fetching sound capsule data: ${e.message}", e)
                }
            } else {
                Log.e("ProfileViewModel", "User ID is null, cannot fetch sound capsule data.")
            }
        }
    }
    
    private fun observeSoundCapsuleData(userId: Int, year: Int, month: Int) {
        // Cancel any existing job
        periodicRefreshJob?.cancel()
        
        // Start Flow collection in a separate coroutine
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Use the new Flow-based method for real-time updates
                soundCapsuleDao.observeSoundCapsuleWithDetails(userId).collect { soundCapsuleData ->
                    val viewModel = convertToViewModel(soundCapsuleData)
                    _soundCapsuleData.value = viewModel
                    
                    // Also refresh the multiple sound capsules whenever the single one updates
                    getMultipleSoundCapsules(userId)
                    
                    Log.d("SoundCapsuleData", "Sound Capsule data updated: ${viewModel.totalTimeListened}ms listened")
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error observing sound capsule data: ${e.message}", e)
            }
        }
        
        // Also set up a periodic refresh as a fallback
        // This ensures data stays fresh even if the Flow observation fails to detect changes
        periodicRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(30000) // Refresh using the defined interval
                try {
                    val latestData = withContext(Dispatchers.IO) {
                        soundCapsuleDao.getSoundCapsuleWithDetails(userId)
                    }
                    val viewModel = convertToViewModel(latestData)
                    
                    // Only update if the data has actually changed
                    val currentValue = _soundCapsuleData.value
                    if (currentValue == null || 
                        currentValue.totalTimeListened != viewModel.totalTimeListened ||
                        currentValue.listeningDayStreak != viewModel.listeningDayStreak) {
                        _soundCapsuleData.value = viewModel
                        
                        // Also refresh the multiple sound capsules when single one changes
                        getMultipleSoundCapsules(userId)
                        
                        Log.d("SoundCapsuleData", "Periodic refresh updated data: ${viewModel.totalTimeListened}ms listened")
                    }
                } catch (e: Exception) {
                    Log.e("ProfileViewModel", "Error in periodic refresh: ${e.message}", e)
                }
            }
        }
    }
    
    private fun convertToViewModel(source: SoundCapsuleDao.SoundCapsuleWithSongs): SoundCapsuleViewModel {
        val capsule = source.soundCapsule
        
        val topSong = source.topSong?.let {
            SoundCapsuleViewModel.TopSongViewModel(
                id = it.id,
                name = it.name,
                artist = it.artist,
                description = it.description,
                duration = it.duration,
                artwork = it.artwork
            )
        }
        
        val streakSongs = source.streakSongs.map {
            SoundCapsuleViewModel.DayStreakSongViewModel(
                songId = it.songId,
                name = it.name,
                artist = it.artist,
                artwork = it.artwork,
                playDate = it.playDate,
                playCount = it.playCount
            )
        }
        
        val topStreakSong = source.topStreakSong?.let {
            SoundCapsuleViewModel.DayStreakSongViewModel(
                songId = it.songId,
                name = it.name,
                artist = it.artist,
                artwork = it.artwork,
                playDate = it.mostRecentDate,
                playCount = it.totalPlayCount,
                dayCount = it.dayCount
            )
        }
        
        val monthYear = if (capsule != null) {
            val localDate = LocalDate.of(capsule.year, capsule.month, 1)
            localDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        } else {
            LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        }
        
        return SoundCapsuleViewModel(
            totalTimeListened = capsule?.totalTimeListened ?: 0,
            topArtist = capsule?.topArtistName,
            topSong = topSong,
            listeningDayStreak = capsule?.listeningDayStreak ?: 0,
            monthYear = monthYear,
            streakSongs = streakSongs,
            topStreakSong = topStreakSong
        )
    }
    
    // New method to get multiple sound capsules
    private suspend fun getMultipleSoundCapsules(userId: Int) {
        try {
            val currentDate = LocalDate.now()
            val soundCapsules = mutableListOf<SoundCapsuleViewModel>()
            
            // Get up to 3 most recent capsules
            // Use the soundCapsuleDao to get the list of all capsules, sorted by year and month
            val allCapsules = soundCapsuleDao.getAllSoundCapsules(userId).first().take(3)
            
            for (capsule in allCapsules) {
                // For each capsule, get the full details and convert to ViewModel
                val details = soundCapsuleDao.getSoundCapsuleWithDetails(userId, capsule.year, capsule.month)
                soundCapsules.add(convertToViewModel(details))
            }
            
            _soundCapsulesData.value = soundCapsules
            Log.d("SoundCapsuleData", "Loaded ${soundCapsules.size} sound capsules")
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Error fetching multiple sound capsules: ${e.message}", e)
        }
    }
    
    fun getProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _isError.value = false
            _noInternet.value = false

            try {
                // First check network connectivity
                if (networkMonitor.isConnected.first()) {
                    // Try to get profile from network
                    val result = repository.getProfile()
                    if (result != null) {
                        _profile.value = result
                        
                        // After successful profile fetch, try to get sound capsule data
                        getSoundCapsule()
                    } else {
                        _isError.value = true
                    }
                } else {
                    _noInternet.value = true
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error in getProfile: ${e.message}", e)
                _isError.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getSongsCount() {
        viewModelScope.launch(Dispatchers.IO) {
            _songsCount.value = repository.getSongsCount()
        }
    }

    fun getFavoriteSongsCount() {
        viewModelScope.launch(Dispatchers.IO) {
            _favoriteCount.value = repository.getSongsLiked()
        }
    }

    fun getTotalListenedCount() {
        viewModelScope.launch(Dispatchers.IO) {
            _playedCount.value = repository.getTotalListened()
        }
    }
    
    fun updateProfile(location: String, profilePhotoUri: Uri?) {
        viewModelScope.launch {
            _updateProfileStatus.value = UpdateProfileStatus.Loading

            if (networkMonitor.isConnected.first()) {
                try {
                    val result = repository.updateProfile(location, profilePhotoUri)
                    result.fold(
                        onSuccess = { message ->
                            _updateProfileStatus.value = UpdateProfileStatus.Success
                            // Refresh profile after successful update
                            getProfile()
                        },
                        onFailure = { error ->
                            _updateProfileStatus.value = UpdateProfileStatus.Error(error.message ?: "Failed to update profile")
                        }
                    )
                } catch (e: Exception) {
                    _updateProfileStatus.value = UpdateProfileStatus.Error(e.message ?: "An error occurred")
                }
            } else {
                _updateProfileStatus.value = UpdateProfileStatus.NoInternet
            }
        }
    }

    fun getCurrentLocation(context: Context, onPermissionDenied: () -> Unit) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    viewModelScope.launch {
                        val countryCode = getCountryCodeFromLocation(context, it)
                        _currentLocation.value = countryCode
                    }
                }
            }.addOnFailureListener {
                onPermissionDenied()
            }
        } catch (e: SecurityException) {
            onPermissionDenied()
        }
    }
    
    private fun getCountryCodeFromLocation(context: Context, location: Location): String {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                    if (addresses.isNotEmpty() && addresses[0].countryCode != null) {
                        _currentLocation.value = addresses[0].countryCode
                    } else {
                        _currentLocation.value = "Unknown"
                    }
                }
                return "Unknown" // Will be updated asynchronously
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                return if (addresses != null && addresses.isNotEmpty() && addresses[0].countryCode != null) {
                    addresses[0].countryCode
                } else {
                    "Unknown"
                }
            }
        } catch (e: Exception) {
            return "Unknown"
        }
    }

    fun resetUpdateStatus() {
        _updateProfileStatus.value = UpdateProfileStatus.Initial
    }

    sealed class UpdateProfileStatus {
        object Initial : UpdateProfileStatus()
        object Loading : UpdateProfileStatus()
        object Success : UpdateProfileStatus()
        object NoInternet : UpdateProfileStatus()
        data class Error(val message: String) : UpdateProfileStatus()
    }

    // Add this at the class level
    private var periodicRefreshJob: Job? = null

    override fun onCleared() {
        super.onCleared()
        periodicRefreshJob?.cancel()
    }
}