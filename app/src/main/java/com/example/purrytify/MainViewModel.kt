package com.example.purrytify

import android.app.Application
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.data.auth.AuthState
import com.example.purrytify.db.AppDatabase
import com.example.purrytify.db.entity.Songs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository.getInstance(application)

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val usersDao = AppDatabase.getDatabase(application).usersDao()

    private val _currentSong = MutableStateFlow<Songs?>(null)
    val currentSong: StateFlow<Songs?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isMiniPlayerActive = MutableStateFlow(false)
    val isMiniPlayerActive: StateFlow<Boolean> = _isMiniPlayerActive



    private var updatePositionJob: kotlinx.coroutines.Job? = null

    private var mediaPlayer: MediaPlayer? = null
    val currentPosition: MutableStateFlow<Int> = MutableStateFlow(0) // Add this property

    init {
        checkLoginStatus()
        observeAuthState()
    }

    private fun activateMiniPlayer() {
        _isMiniPlayerActive.value = true
    }

    private fun deactivateMiniPlayer() {
        _isMiniPlayerActive.value = false
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
                // Force Logout
                _isLoggedIn.value = false
            }
        }
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
    fun playSong(song: Songs) {
        Log.d("IsPlayed Song","Start${_isPlaying.value}")
        if (_currentSong.value != song) {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(getApplication(), Uri.parse(song.filePath))
                prepare()
                start()
                _isPlaying.value = true
                viewModelScope.launch {
                    Log.d("Increment played: ", "${usersDao.getTotalPlayedById(authRepository.currentUserId)}")
                    authRepository.currentUserId?.let { usersDao.incrementTotalPlayed(it) }
                }
                _currentSong.value = song
                setOnCompletionListener {
                    _isPlaying.value = false
                    updatePositionJob?.cancel()
                    deactivateMiniPlayer()
                    _currentSong.value = null
                }
            }
            updateCurrentPosition(song.id)
            activateMiniPlayer()
        } else {
            Log.d("IsPlayed Song","Lagi playing diklik${_isPlaying.value}")

            if (_isPlaying.value) {
                mediaPlayer?.pause()
                updatePositionJob?.cancel()
                _isPlaying.value = false
            } else {
                mediaPlayer?.start()
                _isPlaying.value = true
                updateCurrentPosition(song.id)

            }
        }
    }
    fun stopPlaying() {
        mediaPlayer?.pause()
        _isPlaying.value = false
        updatePositionJob?.cancel()
        deactivateMiniPlayer()
        _currentSong.value = null
    }

    private fun updateCurrentPosition(songId: Int) {
        updatePositionJob?.cancel()
        updatePositionJob = viewModelScope.launch {
            while (mediaPlayer != null && mediaPlayer?.isPlaying == true) {
                val currentMediaPlayerPosition = mediaPlayer?.currentPosition ?: 0
                if (currentSong.value?.id == songId && currentPosition.value != currentMediaPlayerPosition) {
                    currentPosition.value = currentMediaPlayerPosition
                }
                delay(1000)
            }
        }
    }




    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
        _isPlaying.value = false
        _currentSong.value = null
        deactivateMiniPlayer()
    }
    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
        currentPosition.value = position
    }

    override fun onCleared() {
        super.onCleared()
        releaseMediaPlayer()
    }
}