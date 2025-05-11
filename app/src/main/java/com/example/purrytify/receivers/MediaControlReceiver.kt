package com.example.purrytify.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.purrytify.services.MusicPlayerService

/**
 * Receives broadcast intents for media control actions from the notification
 */
class MediaControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("MediaControlReceiver", "Received intent: ${intent.action}")
        
        // Forward the intent to the service
        val serviceIntent = Intent(context, MusicPlayerService::class.java).apply {
            action = intent.action
        }
        context.startService(serviceIntent)
    }
}
