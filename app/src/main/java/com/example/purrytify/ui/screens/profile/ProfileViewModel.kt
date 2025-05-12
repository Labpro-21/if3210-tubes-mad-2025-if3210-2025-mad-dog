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
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

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

    private val authRepository = AuthRepository.getInstance(application)

    private val listenActivityDao = AppDatabase.getDatabase(application).listeningCapsuleDao()
    private val listenActivityRepository = ListeningActivityRepository.getInstance(listenActivityDao)

    fun getProfile() {
        viewModelScope.launch(Dispatchers.IO) { // Gunakan Dispatchers.IO untuk operasi database
            _isLoading.value = true
            _isError.value = false
            _noInternet.value = false
            val userId = authRepository.currentUserId
            if (userId != null) {
                try {
                    val soundCapsuleData = listenActivityRepository.getSoundCapsuleData(userId)
                    Log.d("SoundCapsuleData", "Result from DB: $soundCapsuleData")
                    Log.d(
                        "SoundCapsuleData",
                        "Total Listening Time: ${soundCapsuleData.totalTimeListened}"
                    )
                    Log.d("SoundCapsuleData", "Top Artist: ${soundCapsuleData.topArtist}")
                    Log.d("SoundCapsuleData", "Top Song: ${soundCapsuleData.topSong.toString()}")
                    Log.d("SoundCapsuleData", "ListenedDayStreak: ${soundCapsuleData.listeningDayStreak}")
                } catch (e: Exception) {
                    Log.e("ProfileViewModel", "Error fetching sound capsule data: ${e.message}", e)
                    _isError.value = true // Set error state jika gagal mengambil data dari DB
                }
            } else {
                Log.e("ProfileViewModel", "User ID is null, cannot fetch sound capsule data.")
                _isError.value = true // Set error state jika User ID null
            }

            // Tetap jalankan operasi jaringan di background thread juga
            if (networkMonitor.isConnected.first()) {
                val result = repository.getProfile()
                if (result != null) {
                    _profile.value = result
                } else {
                    _isError.value = true
                }
            } else {
                _noInternet.value = true
            }
            _isLoading.value = false
        }
    }

    fun getSongsCount() {
        viewModelScope.launch(Dispatchers.IO) { // Gunakan Dispatchers.IO untuk operasi database
            _songsCount.value = repository.getSongsCount()
        }
    }

    fun getFavoriteSongsCount() {
        viewModelScope.launch(Dispatchers.IO) { // Gunakan Dispatchers.IO untuk operasi database
            _favoriteCount.value = repository.getSongsLiked()
        }
    }

    fun getTotalListenedCount() {
        viewModelScope.launch(Dispatchers.IO) { // Gunakan Dispatchers.IO untuk operasi database
            _playedCount.value = repository.getTotalListened()
        }
    }
    fun updateProfile(location: String, profilePhotoUri: Uri?) {
        viewModelScope.launch { // Biasanya operasi jaringan tidak dibatasi ke Dispatchers.IO
            _updateProfileStatus.value = UpdateProfileStatus.Loading

            if (networkMonitor.isConnected.first()) {
                try {
                    val result = repository.updateProfile(location, profilePhotoUri)
                    result.fold(
                        onSuccess = { updatedProfile ->
                            _profile.value = updatedProfile
                            _updateProfileStatus.value = UpdateProfileStatus.Success
                            getProfile() // Memanggil getProfile yang sekarang sudah benar
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
                    viewModelScope.launch { // Operasi Geocoder mungkin perlu Dispatchers.IO
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