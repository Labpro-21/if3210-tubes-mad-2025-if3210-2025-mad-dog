package com.example.purrytify

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.data.auth.AuthState
import com.example.purrytify.db.AppDatabase
import com.example.purrytify.db.entity.Songs
import com.example.purrytify.services.AudioPlayerService
import com.example.purrytify.services.AudioPlayerService.PlaybackServiceBinder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository.getInstance(application)
    private val usersDao = AppDatabase.getDatabase(application).usersDao()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _isOnlineSong = MutableStateFlow(false)
    val isOnlineSong: StateFlow<Boolean> = _isOnlineSong

    private val _currentSong = MutableStateFlow<Songs?>(null)
    val currentSong: StateFlow<Songs?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isMiniPlayerActive = MutableStateFlow(false)
    val isMiniPlayerActive: StateFlow<Boolean> = _isMiniPlayerActive

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition

    // Binder untuk berinteraksi dengan AudioPlayerService
    private var serviceBinder: AudioPlayerService.PlaybackServiceBinder? = null
    private var positionUpdateJob: Job? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.d(TAG, "Service Connected")
            serviceBinder = service as PlaybackServiceBinder
            // Pass the current isOnlineSong value to the service
            serviceBinder?.setIsOnlineSong(_isOnlineSong.value)

            // Setelah terhubung, kita bisa mendapatkan status pemutaran dari Service
            updatePlayerStateFromService()
            startPositionUpdates()

            // Observe LiveData from service
            serviceBinder?.getService()?.isPlaying?.observeForever { isPlaying ->
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startPositionUpdates()
                } else {
                    positionUpdateJob?.cancel()
                }
            }

            serviceBinder?.getService()?.currentSongLiveData?.observeForever { song ->
                _currentSong.value = song
                if (song != null) {
                    activateMiniPlayer()
                } else {
                    deactivateMiniPlayer()
                }
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            Log.d(TAG, "Service Disconnected")
            positionUpdateJob?.cancel()
            serviceBinder = null
        }
    }

    init {
        checkLoginStatus()
        observeAuthState()
        bindToAudioPlayerService()
    }

    private fun updatePlayerStateFromService() {
        serviceBinder?.let { binder ->
            _isPlaying.value = binder.isPlaying
            _currentPosition.value = binder.currentPosition
            _currentSong.value = binder.currentSong

            if (binder.currentSong != null) {
                activateMiniPlayer()
            }
        }
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (isActive && _isPlaying.value) {
                serviceBinder?.let { binder ->
                    _currentPosition.value = binder.currentPosition
                }
                delay(500) // Update every 500ms for smoother slider movement
            }
        }
    }

    private fun bindToAudioPlayerService() {
        Intent(getApplication(), AudioPlayerService::class.java).also { intent ->
            getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Important: Remove LiveData observers to prevent memory leaks
        serviceBinder?.getService()?.isPlaying?.removeObserver { }
        serviceBinder?.getService()?.currentSongLiveData?.removeObserver { }
        positionUpdateJob?.cancel()
        getApplication<Application>().unbindService(serviceConnection)
    }

    private fun activateMiniPlayer() {
        _isMiniPlayerActive.value = true
    }

    private fun deactivateMiniPlayer() {
        _isMiniPlayerActive.value = false
    }

    fun setIsOnlineSong(isOnline: Boolean) {
        _isOnlineSong.value = isOnline
        // Update the service with the new value if it's bound
        serviceBinder?.setIsOnlineSong(isOnline)
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            try {
                val loggedIn = authRepository.isLoggedIn()
                _isLoggedIn.value = loggedIn
                Log.d(TAG, "Initial login status: ${_isLoggedIn.value}")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking login status: ${e.message}")
                _isLoggedIn.value = false
            }
        }
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                Log.d(TAG, "Auth state changed: $state")
                when (state) {
                    is AuthState.Authenticated -> {
                        _isLoggedIn.value = true
                    }
                    is AuthState.NotAuthenticated -> {
                        _isLoggedIn.value = false
                        deactivateMiniPlayer()
                        stopPlaying()
                    }
                    else -> {}
                }
            }
        }
    }

    fun setLoggedIn(value: Boolean) {
        _isLoggedIn.value = value
    }

    fun logout() {
        viewModelScope.launch {
            Log.d(TAG, "MainViewModel: Logout called")
            try {
                authRepository.logout()
                Log.d(TAG, "Logout completed, isLoggedIn = ${_isLoggedIn.value}")
            } catch (e: Exception) {
                Log.e(TAG, "Error during logout: ${e.message}")
                _isLoggedIn.value = false
                deactivateMiniPlayer()
                stopPlaying()
            }
        }
    }

    fun playSong(song: Songs) {
        viewModelScope.launch {
            // First stop current playback to avoid conflicts
            if (_isPlaying.value && _currentSong.value != song) {
                serviceBinder?.stopPlayback()
            }

            // Pass isOnlineSong to the startPlayback method
            val activityId = serviceBinder?.startPlayback(song, _isOnlineSong.value)

            if (serviceBinder?.isPlaying == true) {
                activateMiniPlayer()
                // Only increment play count if it's not an online song
                if (!_isOnlineSong.value) {
                    authRepository.currentUserId?.let { usersDao.incrementTotalPlayed(it) }
                }
            }
        }
    }

    fun togglePlayPause() {
        _currentSong.value?.let { song ->
            // Pass isOnlineSong to the startPlayback method
            serviceBinder?.startPlayback(song, _isOnlineSong.value)
        }
    }

    fun stopPlaying() {
        serviceBinder?.stopPlayback()
    }

    fun seekTo(position: Int) {
        serviceBinder?.seekTo(position)
        _currentPosition.value = position
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}