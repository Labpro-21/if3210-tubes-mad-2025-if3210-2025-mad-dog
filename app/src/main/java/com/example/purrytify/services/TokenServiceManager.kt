package com.example.purrytify.services

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.purrytify.workers.TokenValidationWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object TokenServiceManager {
    private const val TOKEN_VALIDATION_WORK_NAME = "token_validation_work"
    private const val TAG = "TokenServiceManager"

    private var workManager: WorkManager? = null

    
    fun startTokenValidationService(context: Context) {
    Log.e(TAG, "Starting token validation service") 
    
    // Create less restrictive constraints
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
            
    // Use OneTimeWorkRequest for immediate testing
    val oneTimeWorkRequest = androidx.work.OneTimeWorkRequestBuilder<TokenValidationWorker>()
        .addTag(TOKEN_VALIDATION_WORK_NAME)
        .setConstraints(constraints)
        .build()
        
    // Enqueue one-time work to run immediately
    WorkManager.getInstance(context).enqueueUniqueWork(
        TOKEN_VALIDATION_WORK_NAME + "_immediate",
        androidx.work.ExistingWorkPolicy.REPLACE,
        oneTimeWorkRequest
    )
    
    // Schedule periodic work with a guaranteed execution
    val periodicWorkRequest = PeriodicWorkRequestBuilder<TokenValidationWorker>(
        15, TimeUnit.MINUTES  // Use 15 minutes - the minimum reliable interval on most Android devices
    )
        .addTag(TOKEN_VALIDATION_WORK_NAME)
        .setConstraints(constraints)
        .setInitialDelay(3, TimeUnit.MINUTES) // Add initial delay after one-time work
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        TOKEN_VALIDATION_WORK_NAME,
        ExistingPeriodicWorkPolicy.REPLACE,
        periodicWorkRequest
    )

    // Set up a repeating immediate check using a chain of one-time works
    scheduleNextOneTimeWork(context)
    
    Log.e(TAG, "Token validation services scheduled")
}

private fun scheduleNextOneTimeWork(context: Context) {
    val delay = 4 * 60 * 1000L // 4 minutes in milliseconds
    
    val oneTimeWorkRequest = androidx.work.OneTimeWorkRequestBuilder<TokenValidationWorker>()
        .addTag(TOKEN_VALIDATION_WORK_NAME + "_chained")
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .build()
        
    WorkManager.getInstance(context).enqueueUniqueWork(
        TOKEN_VALIDATION_WORK_NAME + "_next",
        androidx.work.ExistingWorkPolicy.REPLACE,
        oneTimeWorkRequest
    )
    
    Log.e(TAG, "Scheduled next one-time work in 4 minutes")
}
        
    // Stop the token validation service
    fun stopTokenValidationService(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(TOKEN_VALIDATION_WORK_NAME)
    }
}