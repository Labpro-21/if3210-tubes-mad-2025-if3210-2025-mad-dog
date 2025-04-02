package com.example.purrytify.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.data.auth.TokenManager

class TokenValidationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val authRepository = AuthRepository.getInstance(appContext)

    companion object {
        private const val TAG = "TokenValidationWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting token validation work")
        return try {
            authRepository.verifyToken()
                .onSuccess {
                    Log.d(TAG, "Token verification successful")
                    return Result.success()
                }
                .onFailure { error ->
                    Log.d(TAG, "Token verification failed: ${error.message}")

                    if (error.message?.contains("Token expired") == true) {
                        Log.d(TAG, "Attempting to refresh expired token")

                        authRepository.refreshToken()
                            .onSuccess {
                                Log.d(TAG, "Token refresh successful")
                                return Result.success()
                            }
                            .onFailure { refreshError ->
                                Log.e(TAG, "Token refresh failed: ${refreshError.message}")

                                try {
                                    Log.d(TAG, "Logging out due to refresh token failure")
                                    authRepository.logout()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error during logout: ${e.message}")
                                }

                                return Result.failure()
                            }
                    }
                }

            Log.d(TAG, "Token validation inconclusive, scheduling retry")
            Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in token validation: ${e.message}", e)
            try {
                authRepository.logout()
            } catch (logoutError: Exception) {
                Log.e(TAG, "Error during emergency logout: ${logoutError.message}")
            }
            Result.failure()
        }
    }
}