package com.example.purrytify.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MediaNotificationReceiver : BroadcastReceiver() {
    
    private val TAG = "MediaNotificationReceiver"
    
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Received action: $action")
        
        val mediaController = MediaPlayerController.getInstance(context)
        
        when (action) {
            MediaPlayerController.ACTION_PLAY -> {
                Log.d(TAG, "Processing PLAY action")
                mediaController.play()
            }
            MediaPlayerController.ACTION_PAUSE -> {
                Log.d(TAG, "Processing PAUSE action")
                mediaController.pause()
            }
            MediaPlayerController.ACTION_NEXT -> {
                Log.d(TAG, "Processing NEXT action")
                val currentSong = mediaController.currentSong.value
                if (currentSong != null) {
                    // Call the callback to handle navigation in the ViewModel
                    mediaController.skipToNextCallback?.invoke(currentSong.id)
                    Log.d(TAG, "Next button pressed for song: ${currentSong.id}")
                }
            }
            MediaPlayerController.ACTION_PREVIOUS -> {
                Log.d(TAG, "Processing PREVIOUS action")
                val currentSong = mediaController.currentSong.value
                if (currentSong != null) {
                    // Call the callback to handle navigation in the ViewModel
                    mediaController.skipToPreviousCallback?.invoke(currentSong.id)
                    Log.d(TAG, "Previous button pressed for song: ${currentSong.id}")
                }
            }
        }
    }
}