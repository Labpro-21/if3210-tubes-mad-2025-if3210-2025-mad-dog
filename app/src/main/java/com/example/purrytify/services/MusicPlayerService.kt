package com.example.purrytify.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.purrytify.MainActivity
import com.example.purrytify.R
import com.example.purrytify.db.entity.Songs


class MusicPlayerService : Service() {
    private val binder = LocalBinder()
    private val CHANNEL_ID = "music_playback_channel"
    private val NOTIFICATION_ID = 1

    inner class LocalBinder : Binder() {
        val service: MusicPlayerService
            get() = this@MusicPlayerService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("MusicPlayerService", "Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MusicPlayerService", "Service started with startId: $startId")
        return START_NOT_STICKY
    }

    fun updateNotification(song: Songs?, isPlaying: Boolean) {
        Log.d("MusicPlayerService", "Updating notification: song=${song?.name}, isPlaying=$isPlaying")
        if (song == null) {
            Log.d("MusicPlayerService", "Song is null, stopping foreground")
            stopForeground(STOP_FOREGROUND_REMOVE)
            return
        }

        val notification = createNotification(song, isPlaying)
        Log.d("MusicPlayerService", "Starting foreground with notification")
        startForeground(NOTIFICATION_ID, notification)
    }    private fun createNotification(song: Songs, isPlaying: Boolean): Notification {
        // Create intent for notification tap
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val smallIcon = R.drawable.ic_launcher_foreground
        val largeIcon = if (song.artwork != null) {
            try {
                MediaStore.Images.Media.getBitmap(contentResolver, Uri.parse(song.artwork))
            } catch (e: Exception) {
                Log.e("MusicPlayerService", "Error loading artwork", e)
                BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_foreground)
            }
        } else {
            BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_foreground)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setLargeIcon(largeIcon)
            .setContentTitle(song.name)
            .setContentText(song.artist)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        return builder.build()
    }    private fun createNotificationChannel() {
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
    }
}