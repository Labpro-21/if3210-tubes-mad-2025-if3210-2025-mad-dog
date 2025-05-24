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

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
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
        val streakSongs: List<DayStreakSongViewModel>
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
            val playCount: Int
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
                    
                    // Generate or update the sound capsule data
                    val soundCapsuleResult = soundCapsuleDao.generateAndSaveSoundCapsule(
                        userId
                    )
                    
                    // Convert to ViewModel format
                    val soundCapsuleViewModel = convertToViewModel(soundCapsuleResult)
                    _soundCapsuleData.value = soundCapsuleViewModel
                    
                    Log.d("SoundCapsuleData", "Result from DB: $soundCapsuleViewModel")
                } catch (e: Exception) {
                    Log.e("ProfileViewModel", "Error fetching sound capsule data: ${e.message}", e)
                    // No fallback - just log the error
                }
            } else {
                Log.e("ProfileViewModel", "User ID is null, cannot fetch sound capsule data.")
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
            streakSongs = streakSongs
        )
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
}