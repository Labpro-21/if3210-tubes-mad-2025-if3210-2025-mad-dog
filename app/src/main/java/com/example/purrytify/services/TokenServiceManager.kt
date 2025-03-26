package com.example.purrytify.services

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.purrytify.workers.TokenValidationWorker
import java.util.concurrent.TimeUnit

object TokenServiceManager {
    private const val TOKEN_VALIDATION_WORK_NAME = "token_validation_work"
    
    // Schedule a periodic worker to check token validity
    // We check every 4 minutes since JWT expires in 5 minutes
    fun startTokenValidationService(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<TokenValidationWorker>(
            4, TimeUnit.MINUTES // Check every 4 minutes
        )
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            TOKEN_VALIDATION_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
            workRequest
        )
    }
    
    // Stop the token validation service
    fun stopTokenValidationService(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(TOKEN_VALIDATION_WORK_NAME)
    }
}