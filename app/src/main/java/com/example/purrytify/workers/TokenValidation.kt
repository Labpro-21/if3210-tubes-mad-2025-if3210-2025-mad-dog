package com.example.purrytify.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.data.auth.TokenManager

class TokenValidationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    
    private val tokenManager = TokenManager(appContext)
    private val authRepository = AuthRepository(tokenManager)
    
    override suspend fun doWork(): Result {
        return try {
            // Verify if the token is still valid
            authRepository.verifyToken()
                .onSuccess {
                    // Token is still valid, return success
                    return Result.success()
                }
                .onFailure { error ->
                    if (error.message?.contains("Token expired") == true) {
                        // Token expired, try to refresh it
                        authRepository.refreshToken()
                            .onSuccess {
                                // Refresh successful
                                return Result.success()
                            }
                            .onFailure {
                                // Refresh failed, we need to logout
                                authRepository.logout()
                                return Result.failure()
                            }
                    }
                }
            
            Result.retry()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}