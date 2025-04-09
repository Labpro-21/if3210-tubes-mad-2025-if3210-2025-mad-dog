package com.example.purrytify.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.data.auth.AuthState
import com.example.purrytify.data.auth.TokenManager
import com.example.purrytify.data.auth.TokenUtils
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking

class TokenValidationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val authRepository = AuthRepository.getInstance(appContext)

    companion object {
        private const val TAG = "TokenValidationWorker"
        private const val TOKEN_VALIDATION_WORK_NAME = "token_validation_work"
    }

    override suspend fun doWork(): Result {
        Log.e(TAG, "Token validation worker started - " + System.currentTimeMillis())
        try {
            val accessToken = authRepository.getAccessToken()
            val timeRemaining = TokenUtils.getTimeRemainingInSeconds(accessToken)

            Log.e(TAG, "Current token time remaining: ${timeRemaining}s")

            val result = if (timeRemaining == null || timeRemaining < 90) {
                Log.e(TAG, "Token expiring soon, refreshing...")
                val refreshResult = authRepository.refreshToken()

                if (refreshResult.isSuccess) {
                    Log.e(TAG, "Token refresh successful")
                    Result.success()
                } else {
                    val error = refreshResult.exceptionOrNull()
                    Log.e(TAG, "Token refresh failed: ${error?.message}")
                    try {
                        Log.e(TAG, "Logging out due to refresh token failure")
                        authRepository.logout()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during logout: ${e.message}")
                    }
                    Result.failure()
                }
            } else {
                Log.e(TAG, "Token still valid for ${timeRemaining}s, no refresh needed")
                Result.success()
            }

            // Schedule the next work regardless of success/failure
            scheduleNextWork()
            
            return result

        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in token refresh: ${e.message}", e)
            try {
                scheduleNextWork()
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to schedule next work: ${ex.message}")
            }
            return Result.failure()
        }
    }

    private fun scheduleNextWork() {
    val context = applicationContext
    
    val accessToken = runBlocking { authRepository.getAccessToken() }
    val timeRemaining = TokenUtils.getTimeRemainingInSeconds(accessToken) ?: 60

    val delay = when {
        timeRemaining < 120 -> 30L
        timeRemaining < 180 -> 60L
        timeRemaining < 240 -> 120L
        else -> 240L
    }
    
    Log.e(TAG, "Scheduling next check in ${delay} seconds based on remaining token time of ${timeRemaining}s")
    
    val oneTimeWorkRequest = OneTimeWorkRequestBuilder<TokenValidationWorker>()
        .addTag(TOKEN_VALIDATION_WORK_NAME + "_chained")
        .setInitialDelay(delay, TimeUnit.SECONDS)
        .build()
        
    WorkManager.getInstance(context).enqueueUniqueWork(
        TOKEN_VALIDATION_WORK_NAME + "_next",
        ExistingWorkPolicy.REPLACE,
        oneTimeWorkRequest
    )
}
}