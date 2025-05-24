package com.example.purrytify.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.db.AppDatabase
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Worker to periodically update the Sound Capsule data
 * This ensures the Sound Capsule data is always up-to-date even if the user
 * doesn't open the profile screen frequently
 */
class SoundCapsuleUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SoundCapsuleWorker"
        
        // Update interval in hours
        const val UPDATE_INTERVAL_HOURS = 6L
        
        // Cutoff time in hours (data older than this will be updated)
        const val UPDATE_CUTOFF_HOURS = 12L
    }

    override suspend fun doWork(): Result {
        val appDatabase = AppDatabase.getDatabase(applicationContext)
        val authRepository = AuthRepository.getInstance(applicationContext)
        val userId = authRepository.currentUserId
        
        if (userId == null) {
            Log.d(TAG, "No user logged in, skipping Sound Capsule update")
            return Result.success()
        }
        
        try {
            val soundCapsuleDao = appDatabase.soundCapsuleDao()
            
            // Get current date
            val currentDate = LocalDate.now()
            val year = currentDate.year
            val month = currentDate.monthValue
            
            // Check if we have a Sound Capsule for the current month
            val existingSoundCapsule = soundCapsuleDao.getSoundCapsuleByMonth(userId, year, month)
            
            if (existingSoundCapsule == null) {
                // No Sound Capsule for this month, create a new one
                Log.d(TAG, "Creating new Sound Capsule for $year-$month")
                soundCapsuleDao.generateAndSaveSoundCapsule(userId, null)
            } else {
                // Check if the Sound Capsule is outdated
                val cutoffTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(UPDATE_CUTOFF_HOURS)
                
                if (existingSoundCapsule.lastUpdated < cutoffTime) {
                    // Sound Capsule is outdated, update it
                    Log.d(TAG, "Updating outdated Sound Capsule for $year-$month")
                    soundCapsuleDao.generateAndSaveSoundCapsule(userId, null)
                } else {
                    Log.d(TAG, "Sound Capsule for $year-$month is up-to-date")
                }
            }
            
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating Sound Capsule", e)
            return Result.failure()
        }
    }
} 