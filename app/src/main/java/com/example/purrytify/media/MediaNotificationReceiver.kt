package com.example.purrytify.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MediaNotificationReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "MediaNotificationReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val mediaController = MediaPlayerController.getInstance(context)
        
        Log.d(TAG, "Received action: ${intent.action}")
        
        when (intent.action) {
            MediaPlayerController.ACTION_PLAY -> mediaController.play()
            MediaPlayerController.ACTION_PAUSE -> mediaController.pause()
            MediaPlayerController.ACTION_NEXT -> {
                val currentSong = mediaController.currentSong.value ?: return
                mediaController.skipToNextCallback?.invoke(currentSong.id)
            }
            MediaPlayerController.ACTION_PREVIOUS -> {
                val currentSong = mediaController.currentSong.value ?: return
                mediaController.skipToPreviousCallback?.invoke(currentSong.id)
            }
        }
    }
}
