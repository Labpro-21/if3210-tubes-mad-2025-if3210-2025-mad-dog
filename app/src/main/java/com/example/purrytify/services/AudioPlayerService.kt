package com.example.purrytify.services

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.db.AppDatabase
import com.example.purrytify.db.entity.ListeningActivity
import com.example.purrytify.db.entity.Songs
import com.example.purrytify.data.repository.ListeningActivityRepository
import com.example.purrytify.db.dao.ListeningActivityDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Date

class AudioPlayerService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var listenActivityRepository: ListeningActivityRepository
    private lateinit var listenActivityDao: ListeningActivityDao
    private lateinit var authRepository: AuthRepository
    private val mainHandler = Handler(Looper.getMainLooper())

    private var mediaPlayer: MediaPlayer? = null
    private var currentSong: Songs? = null
    private var currentActivityId: Long? = null
    private var isOnlineSong: Boolean = false

    private val _isPlaying = MutableLiveData<Boolean>()
    val isPlaying: LiveData<Boolean> = _isPlaying

    private val _currentPosition = MutableLiveData<Int>()
    val currentPosition: LiveData<Int> = _currentPosition

    private val _currentSongLiveData = MutableLiveData<Songs?>()
    val currentSongLiveData: LiveData<Songs?> = _currentSongLiveData

    init {
        _isPlaying.value = false
        _currentPosition.value = 0
        _currentSongLiveData.value = null
    }

    private val binder = PlaybackServiceBinder()

    inner class PlaybackServiceBinder : Binder() {
        val isPlaying: Boolean get() = this@AudioPlayerService._isPlaying.value ?: false
        val currentPosition: Int get() = this@AudioPlayerService._currentPosition.value ?: 0
        val currentSong: Songs? get() = this@AudioPlayerService.currentSong
        fun getService(): AudioPlayerService = this@AudioPlayerService
        fun startPlayback(song: Songs, isOnline: Boolean): Long? = this@AudioPlayerService.startPlaybackInternal(song, isOnline)
        fun stopPlayback() = this@AudioPlayerService.stopPlaybackInternal()
        fun seekTo(position: Int) = this@AudioPlayerService.seekToInternal(position)
        fun setIsOnlineSong(isOnline: Boolean) {
            this@AudioPlayerService.isOnlineSong = isOnline
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        // Only initialize these repositories if it's not an online song
        if (!isOnlineSong) {
            listenActivityDao = AppDatabase.getDatabase(applicationContext).listeningCapsuleDao()
            listenActivityRepository = ListeningActivityRepository.getInstance(listenActivityDao)
            authRepository = AuthRepository.getInstance(applicationContext)
        }
    }

    private fun startPlaybackInternal(song: Songs, isOnline: Boolean): Long? {
        // Update the isOnlineSong flag
        this.isOnlineSong = isOnline

        // Initialize repositories if they weren't initialized in onCreate
        if (!isOnlineSong && !::listenActivityDao.isInitialized) {
            listenActivityDao = AppDatabase.getDatabase(applicationContext).listeningCapsuleDao()
            listenActivityRepository = ListeningActivityRepository.getInstance(listenActivityDao)
            authRepository = AuthRepository.getInstance(applicationContext)
        }

        // Handling different scenarios
        if (mediaPlayer != null) {
            if (currentSong?.id == song.id) {
                // Toggle play/pause for the same song
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                    updateIsPlaying(false)
                } else {
                    mediaPlayer?.start()
                    updateIsPlaying(true)
                    startTrackingPosition()
                }
                return currentActivityId
            } else {
                // We're changing songs, so complete the current listening activity
                if (!isOnlineSong) {
                    completeListeningActivity()
                }
                stopMediaPlayer()
            }
        }

        // Start a new song
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, Uri.parse(song.filePath))
                setOnPreparedListener {
                    start()
                    updateIsPlaying(true)
                    currentSong = song
                    updateCurrentSong(song)
                    if (!isOnlineSong) {
                        createListeningActivity(song)
                    }
                    startTrackingPosition()
                }
                setOnCompletionListener {
                    updateIsPlaying(false)
                    if (!isOnlineSong) {
                        completeListeningActivity()
                    }
                    currentSong = null
                    updateCurrentSong(null)
                    stopTrackingPosition()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPlayerService", "MediaPlayer error: what=$what, extra=$extra")
                    stopPlaybackInternal()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerService", "Error starting playback: ${e.message}", e)
            stopPlaybackInternal()
        }

        return currentActivityId
    }

    private fun stopMediaPlayer() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            } catch (e: Exception) {
                Log.e("AudioPlayerService", "Error stopping media player: ${e.message}", e)
            } finally {
                mediaPlayer = null
            }
        }
    }

    private fun createListeningActivity(song: Songs) {
        if (isOnlineSong) return // Skip for online songs

        serviceScope.launch {
            authRepository.currentUserId?.let { userId ->
                val newActivity = ListeningActivity(
                    userId = userId,
                    songId = song.id,
                    startTime = Date(),
                    endTime = null,
                    duration = 0L,
                    completed = false
                )
                currentActivityId = listenActivityRepository.insert(newActivity)
                Log.d("AudioPlayerService", "Created listening activity ID: $currentActivityId for song ID: ${song.id}")
            } ?: run {
                Log.w("AudioPlayerService", "User ID is null, cannot create listening activity.")
            }
        }
    }

    private fun stopPlaybackInternal() {
        if (!isOnlineSong) {
            completeListeningActivity()
        }
        stopTrackingPosition()
        stopMediaPlayer()

        updateIsPlaying(false)
        currentSong = null
        updateCurrentSong(null)
        updateCurrentPosition(0)
        currentActivityId = null
    }

    private fun seekToInternal(position: Int) {
        mediaPlayer?.seekTo(position)
        updateCurrentPosition(position)
    }

    private var isTrackingPosition = false

    private fun startTrackingPosition() {
        if (isTrackingPosition) return

        isTrackingPosition = true
        serviceScope.launch {
            while (isTrackingPosition && mediaPlayer != null) {
                try {
                    if (mediaPlayer?.isPlaying == true) {
                        val position = mediaPlayer?.currentPosition ?: 0
                        updateCurrentPosition(position)
                    }
                    kotlinx.coroutines.delay(500) // Update every 500ms for smoother slider
                } catch (e: Exception) {
                    Log.e("AudioPlayerService", "Error tracking position: ${e.message}")
                    isTrackingPosition = false
                }
            }
        }
    }

    private fun stopTrackingPosition() {
        isTrackingPosition = false
    }

    private fun completeListeningActivity() {
        if (isOnlineSong) return // Skip for online songs

        serviceScope.launch {
            currentActivityId?.let { id ->
                try {
                    val existingActivity = listenActivityDao.getById(id.toInt())
                    existingActivity?.let {
                        val endTime = Date()
                        val actualDuration = endTime.time - it.startTime.time
                        val updatedActivity = it.copy(endTime = endTime, duration = actualDuration, completed = true)
                        listenActivityRepository.update(updatedActivity)
                        Log.d("AudioPlayerService", "Completed listening activity ID: $id, Duration: $actualDuration ms")
                    } ?: run {
                        Log.w("AudioPlayerService", "Listening activity with ID $id not found for completion.")
                    }
                } catch (e: Exception) {
                    Log.e("AudioPlayerService", "Error completing listening activity: ${e.message}")
                }
            }
        }
    }

    // Helper methods to update LiveData on the main thread
    private fun updateIsPlaying(playing: Boolean) {
        mainHandler.post {
            _isPlaying.value = playing
        }
    }

    private fun updateCurrentPosition(position: Int) {
        mainHandler.post {
            _currentPosition.value = position
        }
    }

    private fun updateCurrentSong(song: Songs?) {
        mainHandler.post {
            _currentSongLiveData.value = song
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isOnlineSong) {
            completeListeningActivity()
        }
        stopMediaPlayer()
        serviceScope.cancel()
    }
}