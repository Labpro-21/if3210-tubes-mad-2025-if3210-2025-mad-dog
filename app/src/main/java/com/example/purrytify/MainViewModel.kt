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
import com.example.purrytify.media.MediaPlaybackService
import com.example.purrytify.media.MediaPlayerController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository.getInstance(application)

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _isOnlineSong = MutableStateFlow(false)
    val isOnlineSong: StateFlow<Boolean> = _isOnlineSong

    private val usersDao = AppDatabase.getDatabase(application).usersDao()

    private val _currentSong = MutableStateFlow<Songs?>(null)
    val currentSong: StateFlow<Songs?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isMiniPlayerActive = MutableStateFlow(false)
    val isMiniPlayerActive: StateFlow<Boolean> = _isMiniPlayerActive

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition

    private val _songFinished = MutableStateFlow(false)
    val songFinished: StateFlow<Boolean> = _songFinished

    // For backward compatibility
    private var mediaPlayer: MediaPlayer? = null

    // Service connection
    private var mediaController: MediaPlayerController? = null
    private var serviceConnection: ServiceConnection? = null
    private var serviceBound = false
    private var updatePositionJob: kotlinx.coroutines.Job? = null

    // Navigation callbacks for media controller
    private var skipToNextNavigationCallback: ((Int, Boolean, String, (Int) -> Unit) -> Unit)? = null
    private var skipToPreviousNavigationCallback: ((Int, Boolean, String, (Int) -> Unit) -> Unit)? = null

    fun registerNavigationCallbacks(
        skipToNext: (Int, Boolean, String, (Int) -> Unit) -> Unit,
        skipToPrevious: (Int, Boolean, String, (Int) -> Unit) -> Unit
    ) {
        skipToNextNavigationCallback = skipToNext
        skipToPreviousNavigationCallback = skipToPrevious

        updateMediaControllerCallbacks()
    }

    private fun updateMediaControllerCallbacks() {
        if (serviceBound && mediaController != null) {
            mediaController?.skipToNextCallback = { songId ->
                Log.d(TAG, "MediaController requesting skip to next for song: $songId")
                val isOnline = _isOnlineSong.value
                val region = "GLOBAL"
                skipToNextNavigationCallback?.invoke(songId, isOnline, region) { nextSongId ->
                    Log.d(TAG, "Would navigate to next song: $nextSongId")
                }
            }

            mediaController?.skipToPreviousCallback = { songId ->
                Log.d(TAG, "MediaController requesting skip to previous for song: $songId")
                val isOnline = _isOnlineSong.value
                val region = "GLOBAL"
                skipToPreviousNavigationCallback?.invoke(songId, isOnline, region) { prevSongId ->
                    Log.d(TAG, "Would navigate to previous song: $prevSongId")
                }
            }
        }
    }

    init {
        checkLoginStatus()
        observeAuthState()
        bindMediaService()
    }
    private fun bindMediaService() {
        val context = getApplication<Application>().applicationContext
        val serviceIntent = Intent(context, MediaPlaybackService::class.java)

        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as MediaPlaybackService.LocalBinder
                mediaController = binder.getMediaController()
                serviceBound = true

                mediaController?.setIsOnlineSong(_isOnlineSong.value)

                // Setup callbacks
                mediaController?.setCallbacks(
                    onSkipToNext = { songId -> skipNext(songId) },
                    onSkipToPrevious = { songId -> skipPrevious(songId) },
                    onPlaybackFinished = {
                        _isPlaying.value = false
                        // Don't reset the current song here - we still want to show it in mini player
                        // _currentSong.value = null
                        _songFinished.value = true // Set songFinished to true
                        // Instead, just reset the position and keep mini player active
                        _currentPosition.value = 0

                        // Mini player should remain active to show the completed song
                        //_isMiniPlayerActive.value = false
                    }
                )

                // Update navigation callbacks if they're already registered
                updateMediaControllerCallbacks()

                // Observe controller states
                observeMediaControllerStates()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                mediaController = null
                serviceBound = false
            }
        }

        // Start and bind the service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        context.bindService(serviceIntent, serviceConnection!!, Context.BIND_AUTO_CREATE)
    }

    private fun observeMediaControllerStates() {
        viewModelScope.launch {
            mediaController?.isPlaying?.collectLatest { isPlaying ->
                _isPlaying.value = isPlaying
                // If not playing but we have a current song, ensure mini player is still active
                if (!isPlaying && _currentSong.value != null) {
                    _isMiniPlayerActive.value = true
                }
            }
        }

        viewModelScope.launch {
            mediaController?.currentSong?.collectLatest { song ->
                if (song != null) {
                    // When song changes, ensure the position also resets in UI
                    if (_currentSong.value?.id != song.id) {
                        _currentPosition.value = 0
                    }
                    _currentSong.value = song
                    _isMiniPlayerActive.value = true
                } else {
                    _isMiniPlayerActive.value = false
                    _currentPosition.value = 0
                }
            }
        }

        viewModelScope.launch {
            mediaController?.currentPosition?.collectLatest { position ->
                _currentPosition.value = position
            }
        }
    }

    private fun activateMiniPlayer() {
        _isMiniPlayerActive.value = true
    }

    private fun deactivateMiniPlayer() {
        _isMiniPlayerActive.value = false
    }

    fun setIsOnlineSong(isOnline: Boolean) {
        _isOnlineSong.value = isOnline
        if (serviceBound && mediaController != null) {
            mediaController?.setIsOnlineSong(isOnline)
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
                        stopPlaying()
                    }
                    is AuthState.NotAuthenticated -> {
                        _isLoggedIn.value = false
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
                // The observeAuthState will handle setting _isLoggedIn to false.
                // We now explicitly handle media cleanup here.
                stopPlaying()
                deactivateMiniPlayer()
                // No need to manually set _currentSong to null here,
                // stopPlaying() should handle it.

            } catch (e: Exception) {
                Log.e(TAG, "Error during logout: ${e.message}")
                // Force Logout and ensure media is cleaned up even on error
                _isLoggedIn.value = false
                stopPlaying()
                deactivateMiniPlayer()
            }
        }
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
    fun playSong(song: Songs) {
        Log.d(TAG, "Play song: ${song.name} using media controller")


        if (serviceBound && mediaController != null) {
            mediaController?.setIsOnlineSong(_isOnlineSong.value)
            // Use the media controller if available
            if (_currentSong.value?.id != song.id) {
                // Different song, load it
                mediaController?.loadSong(song)
                //increment
                viewModelScope.launch {
                    authRepository.currentUserId?.let {
                        usersDao.incrementTotalPlayed(it)
                        Log.d(TAG, "Increment played: ${usersDao.getTotalPlayedById(it)}")
                    }
                }
            } else {
                if(_songFinished.value){
                    _songFinished.value = false
                    mediaController?.createActListen(song)
                }
                // Same song, toggle play/pause
                if (_isPlaying.value) {
                    mediaController?.pause()
                } else {
                    // If it's the same song but not playing, we might need to check position
                    if (_currentPosition.value >= (mediaController?.getDuration() ?: 0) - 1000) {
                        // We're at the end, restart
                        mediaController?.seekTo(0)
                    }
                    mediaController?.play()
                }
            }
        } else {
            // Fallback to old implementation
            Log.d(TAG, "Fallback to legacy media player")
            legacyPlaySong(song)
        }
    }

    private fun legacyPlaySong(song: Songs) {
        if (_currentSong.value != song) {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(getApplication(), Uri.parse(song.filePath))
                prepare()
                start()
                _isPlaying.value = true
                _currentSong.value = song
                setOnCompletionListener {
                    _isPlaying.value = false
                    deactivateMiniPlayer()
                    _currentSong.value = null
                }
            }
            updateCurrentPosition(song.id)
            activateMiniPlayer()
        } else {
            if (_isPlaying.value) {
                mediaPlayer?.pause()
                _isPlaying.value = false
            } else {
                mediaPlayer?.start()
                _isPlaying.value = true
                updateCurrentPosition(song.id)
            }
        }
    }    fun stopPlaying() {
        if (serviceBound && mediaController != null) {
            mediaController?.stop()
        } else {
            // Fallback to old implementation
            mediaPlayer?.pause()
            _isPlaying.value = false
            updatePositionJob?.cancel()
            deactivateMiniPlayer()
            _currentSong.value = null
        }
    }    private fun updateCurrentPosition(songId: Int) {
        updatePositionJob?.cancel()
        updatePositionJob = viewModelScope.launch {
            while (mediaPlayer != null && mediaPlayer?.isPlaying == true) {
                val currentMediaPlayerPosition = mediaPlayer?.currentPosition ?: 0
                if (currentSong.value?.id == songId && _currentPosition.value != currentMediaPlayerPosition) {
                    _currentPosition.value = currentMediaPlayerPosition
                }
                delay(1000)
            }
        }
    }

    private fun releaseMediaPlayer() {
        Log.d(TAG, "Releasing legacy MediaPlayer")
        mediaPlayer?.release()
        mediaPlayer = null
        _isPlaying.value = false
        _currentSong.value = null
        _currentPosition.value = 0
    }


    fun seekTo(position: Int) {
        if (serviceBound && mediaController != null) {
            mediaController?.seekTo(position)
        } else {
            mediaPlayer?.seekTo(position)
            _currentPosition.value = position
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "MainViewModel: onCleared called")
        if (serviceBound && serviceConnection != null) {
            val context = getApplication<Application>().applicationContext
            try {
                context.unbindService(serviceConnection!!)
                serviceConnection = null
                serviceBound = false
                mediaController = null
                Log.d(TAG, "Media service unbound")
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Error unbinding service: ${e.message}")
            }
        }
        // Legacy cleanup (ensure it's still done if service wasn't bound)
        releaseMediaPlayer()
    }



    private fun skipNext(songId: Int) {
        if (skipToNextNavigationCallback != null) {
            val isOnline = _isOnlineSong.value
            val region = "GLOBAL"
            skipToNextNavigationCallback?.invoke(songId, isOnline, region) { nextSongId ->
                Log.d(TAG, "Would navigate to next song: $nextSongId")
            }
        } else {
            Log.d(TAG, "Skip to next requested but no navigation callback is registered")
        }
    }

    private fun skipPrevious(songId: Int) {
        if (skipToPreviousNavigationCallback != null) {
            val isOnline = _isOnlineSong.value
            val region = "GLOBAL"
            skipToPreviousNavigationCallback?.invoke(songId, isOnline, region) { prevSongId ->
                Log.d(TAG, "Would navigate to previous song: $prevSongId")
            }
        } else {
            Log.d(TAG, "Skip to previous requested but no navigation callback is registered")
        }
    }
}