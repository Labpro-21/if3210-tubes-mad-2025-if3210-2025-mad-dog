package com.example.purrytify.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.example.purrytify.MainActivity
import com.example.purrytify.MainViewModel
import com.example.purrytify.R
import com.example.purrytify.db.entity.Songs
import com.example.purrytify.ui.screens.songdetail.SongDetailViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MusicPlayerService : Service() {
    private val binder = LocalBinder()
    private val CHANNEL_ID = "music_playback_channel"
    private val NOTIFICATION_ID = 1

    // Add ViewModel references
    private var mainViewModel: MainViewModel? = null
    private var songDetailViewModel: SongDetailViewModel? = null
    private var mediaSession: MediaSessionCompat? = null
    private var isFirstNotification = true

    // Create a coroutine scope for the service
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    inner class LocalBinder : Binder() {
        val service: MusicPlayerService
            get() = this@MusicPlayerService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }    override fun onCreate() {
        super.onCreate()
        Log.d("MusicPlayerService", "Service created")
        createNotificationChannel()

        // Create media session with MediaSessionCompat
        mediaSession = MediaSessionCompat(this, "PurrytifyMediaSession")
        mediaSession?.isActive = true

        // Set up media session callbacks
        mediaSession?.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                super.onPlay()
                Log.d("MusicPlayerService", "MediaSession onPlay")
                handlePlayPause()
            }

            override fun onPause() {
                super.onPause()
                Log.d("MusicPlayerService", "MediaSession onPause")
                handlePlayPause()
            }

            override fun onSkipToNext() {
                super.onSkipToNext()
                Log.d("MusicPlayerService", "MediaSession onSkipToNext")
                handleNext()
            }

            override fun onSkipToPrevious() {
                super.onSkipToPrevious()
                Log.d("MusicPlayerService", "MediaSession onSkipToPrevious")
                handlePrevious()
            }

            override fun onStop() {
                super.onStop()
                Log.d("MusicPlayerService", "MediaSession onStop")
                handleStop()
            }

            override fun onSeekTo(pos: Long) {
                super.onSeekTo(pos)
                Log.d("MusicPlayerService", "MediaSession onSeekTo: $pos")
                mainViewModel?.seekTo(pos.toInt())
            }
        })
    }

    fun setViewModels(mainViewModel: MainViewModel, songDetailViewModel: SongDetailViewModel) {
        this.mainViewModel = mainViewModel
        this.songDetailViewModel = songDetailViewModel
        Log.d("MusicPlayerService", "ViewModels set")
    }    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MusicPlayerService", "Service started with startId: $startId, action: ${intent?.action}")

        // Handle media button events
        MediaButtonReceiver.handleIntent(mediaSession, intent)

        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                Log.d("MusicPlayerService", "Received PLAY_PAUSE action")
                handlePlayPause()
            }
            ACTION_PREV -> {
                Log.d("MusicPlayerService", "Received PREV action")
                handlePrevious()
            }
            ACTION_NEXT -> {
                Log.d("MusicPlayerService", "Received NEXT action")
                handleNext()
            }
            ACTION_STOP -> {
                Log.d("MusicPlayerService", "Received STOP action")
                handleStop()
            }
        }

        return START_NOT_STICKY
    }fun updateNotification(song: Songs?, isPlaying: Boolean) {
        Log.d("MusicPlayerService", "Updating notification: song=${song?.name}, isPlaying=$isPlaying")
        if (song == null) {
            Log.d("MusicPlayerService", "Song is null, stopping foreground")
            stopForeground(STOP_FOREGROUND_REMOVE)
            return
        }

        // Update playback state
        val state = if (isPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }

        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_SEEK_TO
            )
            .setState(state, mainViewModel?.currentPosition?.value?.toLong() ?: 0, 1.0f)

        mediaSession?.setPlaybackState(stateBuilder.build())

        val notification = createNotification(song, isPlaying)
        
        // Use NotificationManager instead of startForeground for updates
        // This prevents the notification from flickering
        if (isFirstNotification) {
            Log.d("MusicPlayerService", "Starting foreground with notification")
            startForeground(NOTIFICATION_ID, notification)
            isFirstNotification = false
        } else {
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }    private fun createNotification(song: Songs, isPlaying: Boolean): Notification {
        // Main intent for tapping notification
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create broadcast intents for the media actions
        val playIntent = Intent(ACTION_PLAY_PAUSE)
        val prevIntent = Intent(ACTION_PREV)
        val nextIntent = Intent(ACTION_NEXT) 
        val stopIntent = Intent(ACTION_STOP)

        // Create pending intents with unique request codes
        val playPendingIntent = PendingIntent.getBroadcast(
            this, 100, playIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val prevPendingIntent = PendingIntent.getBroadcast(
            this, 101, prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nextPendingIntent = PendingIntent.getBroadcast(
            this, 102, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopPendingIntent = PendingIntent.getBroadcast(
            this, 103, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Load artwork
        val artworkBitmap = if (song.artwork != null) {
            try {
                MediaStore.Images.Media.getBitmap(contentResolver, Uri.parse(song.artwork))
            } catch (e: Exception) {
                Log.e("MusicPlayerService", "Error loading artwork", e)
                BitmapFactory.decodeResource(resources, R.drawable.ic_music_placeholder)
            }
        } else {
            BitmapFactory.decodeResource(resources, R.drawable.ic_music_placeholder)
        }

        // Calculate progress percentage for the progress bar
        val currentPositionMs = mainViewModel?.currentPosition?.value ?: 0
        val durationMs = song.duration.toInt()
        val progress = if (durationMs > 0) (currentPositionMs * 100 / durationMs) else 0
        
        // Format current position and duration
        val formattedCurrentPosition = formatTime(currentPositionMs)
        val formattedDuration = formatTime(durationMs)
        
        // Build notification with media style and Spotify-like appearance
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_placeholder)
            .setLargeIcon(artworkBitmap)
            .setContentTitle(song.name)
            .setContentText(song.artist)
            .setSubText("$formattedCurrentPosition / $formattedDuration")
            .setContentIntent(mainPendingIntent)
            .setProgress(100, progress, false) // Add a progress bar
            .setStyle(
                MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(mediaSession?.sessionToken)
            )
            .addAction(R.drawable.ic_skip_previous, "Previous", prevPendingIntent)
            .addAction(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (isPlaying) "Pause" else "Play",
                playPendingIntent
            )
            .addAction(R.drawable.ic_skip_next, "Next", nextPendingIntent)
            .addAction(R.drawable.ic_close, "Stop", stopPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setColorized(true) // Make the notification more colorful like Spotify
            .setColor(resources.getColor(android.R.color.black, null)) // Spotify uses a dark theme
            .build()
    }    private fun handlePlayPause() {
        Log.d("MusicPlayerService", "Handle play/pause")
        mainViewModel?.currentSong?.value?.let { song ->
            // Toggle the playback state
            val isCurrentlyPlaying = mainViewModel?.isPlaying?.value ?: false
            Log.d("MusicPlayerService", "Current playing state: $isCurrentlyPlaying, toggling to ${!isCurrentlyPlaying}")
            
            // Just use playSong which already has the toggle logic
            mainViewModel?.playSong(song)
        } ?: Log.e("MusicPlayerService", "No current song to play/pause")
    }private fun handlePrevious() {
        Log.d("MusicPlayerService", "Handle previous")
        mainViewModel?.currentSong?.value?.let { currentSong ->
            songDetailViewModel?.skipPrevious(currentSong.id) { prevSongId ->
                // Load previous song
                serviceScope.launch {
                    Log.d("MusicPlayerService", "Loading previous song ID: $prevSongId")
                    mainViewModel?.playSongById(prevSongId)
                }
            }
        }
    }

    private fun handleNext() {
        Log.d("MusicPlayerService", "Handle next")
        mainViewModel?.currentSong?.value?.let { currentSong ->
            songDetailViewModel?.skipNext(currentSong.id) { nextSongId ->
                // Load next song
                serviceScope.launch {
                    Log.d("MusicPlayerService", "Loading next song ID: $nextSongId")
                    mainViewModel?.playSongById(nextSongId)
                }
            }
        }
    }

    private fun handleStop() {
        Log.d("MusicPlayerService", "Handle stop")
        mainViewModel?.stopPlaying()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Music Playback",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Shows currently playing music"
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
        Log.d("MusicPlayerService", "Notification channel created")
    }    private fun formatTime(timeInMs: Int): String {
        val totalSeconds = timeInMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.example.purrytify.PLAY_PAUSE"
        const val ACTION_PREV = "com.example.purrytify.PREVIOUS"
        const val ACTION_NEXT = "com.example.purrytify.NEXT"
        const val ACTION_STOP = "com.example.purrytify.STOP"
    }    override fun onDestroy() {
        super.onDestroy()
        Log.d("MusicPlayerService", "Service destroyed, releasing resources")
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
    }
}