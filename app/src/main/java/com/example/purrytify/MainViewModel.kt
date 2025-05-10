package com.example.purrytify

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.data.auth.AuthState
import com.example.purrytify.db.AppDatabase
import com.example.purrytify.db.entity.Songs
import com.example.purrytify.services.MusicPlayerService
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
    val currentPosition: MutableStateFlow<Int> = MutableStateFlow(0) 

    private var musicService: MusicPlayerService? = null
    fun startMusicService(mainActivity: MainActivity) {
        Log.d("MainViewModel", "Starting music service")
        val serviceIntent = Intent(getApplication(), MusicPlayerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(serviceIntent)
        } else {
            getApplication<Application>().startService(serviceIntent)
        }
    }

    fun updateMusicNotification(song: Songs) {
        musicService?.updateNotification(song, _isPlaying.value)
    }

    init {
        checkLoginStatus()
        observeAuthState()
    }

    private fun activateMiniPlayer() {
        _isMiniPlayerActive.value = true
    }    private fun deactivateMiniPlayer() {
        _isMiniPlayerActive.value = false
    }
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d("MainViewModel", "Service connected")
            val binder = service as? MusicPlayerService.LocalBinder
            musicService = binder?.service
            currentSong.value?.let { song ->
                Log.d("MainViewModel", "Updating notification with current song: ${song.name}")
                musicService?.updateNotification(song, isPlaying.value)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("MainViewModel", "Service disconnected")
            musicService = null
        }
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
                        // Tambahkan logika untuk mematikan miniplayer saat login berhasil
                        deactivateMiniPlayer()
                        stopPlaying() // Opsional: Hentikan juga pemutaran saat login
                    }
                    is AuthState.NotAuthenticated -> {
                        _isLoggedIn.value = false
                        deactivateMiniPlayer() // Opsional: Matikan juga saat logout
                        stopPlaying()       // Opsional: Hentikan juga pemutaran saat logout
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
                // Status NotAuthenticated akan di-emit oleh authRepository,
                // dan logika di observeAuthState akan menangani deaktivasi miniplayer.
            } catch (e: Exception) {
                Log.e(TAG, "Error during logout: ${e.message}")
                // Force Logout
                _isLoggedIn.value = false
                deactivateMiniPlayer()
                stopPlaying()
            }
        }
    }

    companion object {
        private const val TAG = "MainViewModel"
    }    fun playSong(song: Songs) {
        Log.d("MainViewModel", "Playing song: ${song.name}, current song: ${_currentSong.value?.name}")
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
            
            // Update notification after everything is set up
            Log.d("MainViewModel", "Updating notification for new song: ${song.name}")
            musicService?.updateNotification(song, true)
        } else {
            Log.d("IsPlayed Song", "Lagi playing diklik${_isPlaying.value}")
            if (_isPlaying.value) {
                mediaPlayer?.pause()
                updatePositionJob?.cancel()
                _isPlaying.value = false
                // Update notification when paused
                musicService?.updateNotification(song, false)
            } else {
                mediaPlayer?.start()
                _isPlaying.value = true
                updateCurrentPosition(song.id)
                // Update notification when resumed
                musicService?.updateNotification(song, true)
            }
        }
    }

    fun stopPlaying() {
        mediaPlayer?.pause()
        _isPlaying.value = false
        updatePositionJob?.cancel()
        deactivateMiniPlayer()
        _currentSong.value = null
        musicService?.updateNotification(null, false)

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
        musicService?.updateNotification(null, false)
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
        currentPosition.value = position
    }    fun bindMusicService(context: Context) {
        Log.d("MainViewModel", "Binding music service")
        val intent = Intent(context, MusicPlayerService::class.java)
        try {
            val bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            Log.d("MainViewModel", "Service binding result: $bound")
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error binding service", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unbindService(serviceConnection)
            Log.d("MainViewModel", "Service unbound in onCleared")
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error unbinding service", e)
        }
    }
}