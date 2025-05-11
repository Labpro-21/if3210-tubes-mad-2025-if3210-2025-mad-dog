package com.example.purrytify.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.purrytify.MainActivity
import com.example.purrytify.R
import com.example.purrytify.db.entity.Songs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaPlayerController private constructor(private val context: Context) {    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "media_playback_channel"
        private var instance: MediaPlayerController? = null
        private const val TAG = "MediaPlayerController"

        const val ACTION_PLAY = "com.example.purrytify.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.purrytify.ACTION_PAUSE"
        const val ACTION_NEXT = "com.example.purrytify.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.purrytify.ACTION_PREVIOUS"

        fun getInstance(context: Context): MediaPlayerController {
            return instance ?: synchronized(this) {
                instance ?: MediaPlayerController(context.applicationContext).also { instance = it }
            }
        }
    }

    private val mediaPlayer = MediaPlayer()
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var audioManager: AudioManager
    private lateinit var audioFocusRequest: AudioFocusRequest
    private val coroutineScope = CoroutineScope(Dispatchers.Main)    // Track states
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSong = MutableStateFlow<Songs?>(null)
    val currentSong: StateFlow<Songs?> = _currentSong.asStateFlow()

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()

    private val _playlist = MutableStateFlow<List<Songs>>(emptyList())
    val playlist: StateFlow<List<Songs>> = _playlist.asStateFlow()

    // Callbacks that can be set from outside
    var skipToNextCallback: ((Int) -> Unit)? = null
    var skipToPreviousCallback: ((Int) -> Unit)? = null
    var playbackFinishedCallback: (() -> Unit)? = null
    
    private var updatePositionJob: kotlinx.coroutines.Job? = null

    init {
        setupMediaPlayer()
        setupMediaSession()
        setupNotificationChannel()
        setupAudioFocus()
    }

    private fun setupMediaPlayer() {
        mediaPlayer.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )

        mediaPlayer.setOnCompletionListener {
            _isPlaying.value = false
            updatePositionJob?.cancel()
            playbackFinishedCallback?.invoke()
        }

        mediaPlayer.setOnPreparedListener {
            play()
        }
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(context, "PurrytifyMediaSession")
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                play()
            }

            override fun onPause() {
                pause()
            }

            override fun onSkipToNext() {
                val currentSong = _currentSong.value ?: return
                skipToNextCallback?.invoke(currentSong.id)
            }

            override fun onSkipToPrevious() {
                val currentSong = _currentSong.value ?: return
                skipToPreviousCallback?.invoke(currentSong.id)
            }

            override fun onStop() {
                stop()
            }

            override fun onSeekTo(pos: Long) {
                seekTo(pos.toInt())
            }
        })
        mediaSession.isActive = true
    }

    private fun setupNotificationChannel() {
        notificationManager = NotificationManagerCompat.from(context)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for currently playing music"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupAudioFocus() {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS -> pause()
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
                        AudioManager.AUDIOFOCUS_GAIN -> play()
                    }
                }
                .build()
        }
    }

    fun setCallbacks(
        onSkipToNext: (Int) -> Unit,
        onSkipToPrevious: (Int) -> Unit,
        onPlaybackFinished: () -> Unit
    ) {
        skipToNextCallback = onSkipToNext
        skipToPreviousCallback = onSkipToPrevious
        playbackFinishedCallback = onPlaybackFinished
    }

    fun setPlaylist(songs: List<Songs>, startIndex: Int = 0) {
        _playlist.value = songs
        if (songs.isNotEmpty() && startIndex < songs.size) {
            loadSong(songs[startIndex])
        }
    }

    fun loadSong(song: Songs) {
        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(context, Uri.parse(song.filePath))
            mediaPlayer.prepareAsync()
            _currentSong.value = song
            updateMetadata(song)
            updateNotification()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading song: ${e.message}")
            e.printStackTrace()
        }
    }

    fun play() {
        val currentSong = _currentSong.value ?: return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val result = audioManager.requestAudioFocus(audioFocusRequest)
            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS -> pause()
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
                        AudioManager.AUDIOFOCUS_GAIN -> play()
                    }
                },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return
        }

        mediaPlayer.start()
        _isPlaying.value = true
        
        val playbackState = PlaybackStateCompat.Builder()
            .setState(
                PlaybackStateCompat.STATE_PLAYING,
                mediaPlayer.currentPosition.toLong(),
                1.0f
            )
            .setActions(
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            .build()
        
        mediaSession.setPlaybackState(playbackState)
        updateNotification()
        updateCurrentPosition()
    }

    fun pause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            _isPlaying.value = false
            
            val playbackState = PlaybackStateCompat.Builder()
                .setState(
                    PlaybackStateCompat.STATE_PAUSED,
                    mediaPlayer.currentPosition.toLong(),
                    0f
                )
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
                .build()
            
            mediaSession.setPlaybackState(playbackState)
            updateNotification()
            updatePositionJob?.cancel()
        }
    }

    fun stop() {
        mediaPlayer.stop()
        _isPlaying.value = false
        clearNotification()
        updatePositionJob?.cancel()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    fun seekTo(position: Int) {
        mediaPlayer.seekTo(position)
        _currentPosition.value = position
    }

    fun getCurrentPosition(): Int {
        return mediaPlayer.currentPosition
    }

    fun getDuration(): Int {
        return mediaPlayer.duration
    }

    private suspend fun getBitmapFromUri(uri: Uri?): Bitmap? {
        if (uri == null) return null
        
        return try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(uri)
                .allowHardware(false)
                .build()
                
            val result = (loader.execute(request) as? SuccessResult)?.drawable
            if (result != null) {
                (result as? android.graphics.drawable.BitmapDrawable)?.bitmap
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap: ${e.message}")
            null
        }
    }

    private fun updateMetadata(song: Songs) {
        coroutineScope.launch {
            val artworkUri = song.artwork?.let { Uri.parse(it) }
            val bitmap = artworkUri?.let { getBitmapFromUri(it) }
                ?: BitmapFactory.decodeResource(context.resources, R.drawable.music_placeholder)
            
            val metadata = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.name)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, song.id.toString())
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                .build()
            
            mediaSession.setMetadata(metadata)
        }
    }

    private fun updateNotification() {
        val currentSong = _currentSong.value ?: return
        
        coroutineScope.launch {
            val notification = buildNotification(currentSong)
            try {
                notificationManager.notify(NOTIFICATION_ID, notification)
            } catch (e: SecurityException) {
                Log.e(TAG, "No permission to show notification: ${e.message}")
            }
        }
    }

    private suspend fun buildNotification(song: Songs): Notification {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val artworkUri = song.artwork?.let { Uri.parse(it) }
        val bitmap = artworkUri?.let { getBitmapFromUri(it) }
            ?: BitmapFactory.decodeResource(context.resources, R.drawable.music_placeholder)

        val playPauseAction = if (_isPlaying.value) {
            NotificationCompat.Action(
                R.drawable.ic_pause, "Pause",
                getPendingIntent(ACTION_PAUSE)
            )
        } else {
            NotificationCompat.Action(
                R.drawable.ic_play, "Play",
                getPendingIntent(ACTION_PLAY)
            )
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setLargeIcon(bitmap)
            .setContentTitle(song.name)
            .setContentText(song.artist)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_skip_previous, "Previous",
                getPendingIntent(ACTION_PREVIOUS)
            )
            .addAction(playPauseAction)
            .addAction(
                R.drawable.ic_skip_next, "Next",
                getPendingIntent(ACTION_NEXT)
            )
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun clearNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun getPendingIntent(action: String): PendingIntent {
        val intent = Intent(action).apply {
            setPackage(context.packageName)
        }
        return PendingIntent.getBroadcast(
            context, 
            action.hashCode(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun updateCurrentPosition() {
        updatePositionJob?.cancel()
        updatePositionJob = coroutineScope.launch {
            while (mediaPlayer.isPlaying) {
                _currentPosition.value = mediaPlayer.currentPosition
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    fun release() {
        mediaPlayer.release()
        mediaSession.release()
        clearNotification()
        updatePositionJob?.cancel()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
        
        instance = null
    }
}
