package com.example.purrytify.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.db.AppDatabase
import com.example.purrytify.db.entity.SoundCapsule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val appDatabase = AppDatabase.getDatabase(applicationContext)
        val authRepository = AuthRepository.getInstance(applicationContext)
        val userId = authRepository.currentUserId
        
        if (userId == null) {
            Log.d(TAG, "No user logged in, skipping Sound Capsule update")
            return@withContext Result.success()
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
                val newCapsuleData = soundCapsuleDao.generateAndSaveSoundCapsule(userId, null)
                Log.d(TAG, "Created new Sound Capsule with ${newCapsuleData.soundCapsule?.totalTimeListened ?: 0}ms listened time")
            } else {
                // Check if the Sound Capsule is outdated
                val cutoffTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(UPDATE_CUTOFF_HOURS)
                
                if (existingSoundCapsule.lastUpdated < cutoffTime) {
                    // Sound Capsule is outdated, update it
                    Log.d(TAG, "Updating outdated Sound Capsule for $year-$month")
                    reconcileRealTimeData(soundCapsuleDao, userId, existingSoundCapsule)
                } else {
                    // Still do a quick reconciliation to ensure data consistency
                    Log.d(TAG, "Sound Capsule for $year-$month is up-to-date, performing light reconciliation")
                    reconcileRealTimeData(soundCapsuleDao, userId, existingSoundCapsule, lightReconciliation = true)
                }
            }
            
            return@withContext Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating Sound Capsule", e)
            return@withContext Result.failure()
        }
    }
    
    private suspend fun reconcileRealTimeData(
        soundCapsuleDao: com.example.purrytify.db.dao.SoundCapsuleDao,
        userId: Int,
        existingSoundCapsule: SoundCapsule,
        lightReconciliation: Boolean = false
    ) {
        try {
            // Get fresh total listening time
            val freshTotalTime = soundCapsuleDao.getTotalListeningTimeThisMonth(userId)
            
            // If real-time update is significantly different from fresh calculation (more than 5% difference)
            // or if it's a full reconciliation, perform a full refresh
            val timeDifference = Math.abs(freshTotalTime - existingSoundCapsule.totalTimeListened)
            val percentDifference = if (existingSoundCapsule.totalTimeListened > 0) {
                (timeDifference.toDouble() / existingSoundCapsule.totalTimeListened.toDouble()) * 100.0
            } else {
                100.0
            }
            
            if (!lightReconciliation || percentDifference > 5.0) {
                Log.d(TAG, "Significant difference detected (${percentDifference.toInt()}%), performing full refresh")
                // Generate completely fresh data
                soundCapsuleDao.generateAndSaveSoundCapsule(userId, null, forceRegenerateStreakSongs = true)
            } else if (freshTotalTime != existingSoundCapsule.totalTimeListened) {
                // Just update the total time if it's different but not significantly
                Log.d(TAG, "Small difference detected, updating total time only")
                val updatedCapsule = existingSoundCapsule.copy(
                    totalTimeListened = freshTotalTime,
                    lastUpdated = System.currentTimeMillis()
                )
                soundCapsuleDao.updateSoundCapsule(updatedCapsule)
            } else {
                Log.d(TAG, "No time difference detected")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reconciling real-time data", e)
            // Still try to do a full refresh as fallback
            soundCapsuleDao.generateAndSaveSoundCapsule(userId, null)
        }
    }
} 