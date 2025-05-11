package com.example.purrytify.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.purrytify.MainActivity
import com.example.purrytify.R

class MediaPlaybackService : Service() {
    
    private val binder = LocalBinder()
    private lateinit var mediaController: MediaPlayerController
    
    companion object {
        private const val CHANNEL_ID = "media_service_channel"
        private const val SERVICE_ID = 101
        private const val TAG = "MediaPlaybackService"
    }
    
    inner class LocalBinder : Binder() {
        fun getService(): MediaPlaybackService = this@MediaPlaybackService
        fun getMediaController(): MediaPlayerController = mediaController
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        mediaController = MediaPlayerController.getInstance(this)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Playback Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Service for media playback"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        
        startForeground(SERVICE_ID, createNotification())
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent): IBinder {
        return binder
    }
    
    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        mediaController.release()
        super.onDestroy()
    }
    
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Purrytify Music Player")
            .setContentText("Music service is running")
            .setSmallIcon(R.drawable.music_placeholder)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
}
