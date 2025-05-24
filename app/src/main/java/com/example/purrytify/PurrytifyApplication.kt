package com.example.purrytify

import NetworkMonitor
import android.app.Application
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.data.auth.TokenManager
import com.example.purrytify.services.TokenServiceManager
import com.example.purrytify.workers.SoundCapsuleUpdateWorker
import java.util.concurrent.TimeUnit

class PurrytifyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize singletons
        TokenManager.getInstance(this)
        AuthRepository.getInstance(this)
        TokenServiceManager.startTokenValidationService(this)
        NetworkMonitor.initialize(this)

        // Schedule periodic Sound Capsule update
        scheduleSoundCapsuleUpdates()

        Log.d("PurrytifyApp", "Application initialized")
    }
    
    private fun scheduleSoundCapsuleUpdates() {
        val soundCapsuleWorkRequest = PeriodicWorkRequestBuilder<SoundCapsuleUpdateWorker>(
            SoundCapsuleUpdateWorker.UPDATE_INTERVAL_HOURS, TimeUnit.HOURS
        ).build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "sound_capsule_update_work",
            ExistingPeriodicWorkPolicy.KEEP,
            soundCapsuleWorkRequest
        )
        
        Log.d("PurrytifyApp", "Sound Capsule update worker scheduled")
    }
}