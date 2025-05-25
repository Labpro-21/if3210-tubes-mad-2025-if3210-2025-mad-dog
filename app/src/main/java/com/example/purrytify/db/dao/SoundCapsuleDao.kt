package com.example.purrytify.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.purrytify.db.entity.DayStreakSong
import com.example.purrytify.db.entity.SoundCapsule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Dao
interface SoundCapsuleDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSoundCapsule(soundCapsule: SoundCapsule): Long
    
    @Update
    suspend fun updateSoundCapsule(soundCapsule: SoundCapsule)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDayStreakSong(dayStreakSong: DayStreakSong): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDayStreakSongs(dayStreakSongs: List<DayStreakSong>)
    
    @Query("SELECT * FROM sound_capsules WHERE userId = :userId AND year = :year AND month = :month")
    suspend fun getSoundCapsuleByMonth(userId: Int, year: Int, month: Int): SoundCapsule?
    
    @Query("SELECT * FROM sound_capsules WHERE userId = :userId ORDER BY year DESC, month DESC LIMIT 1")
    suspend fun getLatestSoundCapsule(userId: Int): SoundCapsule?
    
    @Query("SELECT * FROM sound_capsules WHERE userId = :userId ORDER BY year DESC, month DESC")
    fun getAllSoundCapsules(userId: Int): Flow<List<SoundCapsule>>
    
    @Query("SELECT * FROM day_streak_songs WHERE capsuleId = :capsuleId ORDER BY date DESC")
    suspend fun getDayStreakSongsByCapsuleId(capsuleId: Int): List<DayStreakSong>

    @Query("SELECT * FROM day_streak_songs WHERE capsuleId = :capsuleId ORDER BY date DESC")
    fun observeDayStreakSongs(capsuleId: Int): Flow<List<DayStreakSong>>
    
    /**
     * Gets the top streak song based on play count for display on the SoundCapsule card.
     * This query does the following:
     * 1. Groups songs by songId to count how many days they've been played
     * 2. Filters to only include songs played for at least 2 days
     * 3. Orders by total play count (highest first) and then by number of days (highest first)
     * 4. Returns only the top result
     */
    @Query("""
        SELECT 
            ds.songId,
            s.name,
            s.artist,
            s.artwork,
            MAX(ds.date) as mostRecentDate,
            SUM(ds.playCount) as totalPlayCount,
            COUNT(DISTINCT ds.date) as dayCount
        FROM day_streak_songs ds
        LEFT JOIN songs s ON ds.songId = s.id
        WHERE ds.capsuleId = :capsuleId
        GROUP BY ds.songId
        HAVING COUNT(DISTINCT ds.date) >= 2
        ORDER BY totalPlayCount DESC, dayCount DESC
        LIMIT 1
    """)
    suspend fun getTopStreakSongForDisplay(capsuleId: Int): TopStreakSong?

    data class TopStreakSong(
        val songId: Int?,
        val name: String?,
        val artist: String?,
        val artwork: String?,
        val mostRecentDate: String,
        val totalPlayCount: Int,
        val dayCount: Int
    )
    
    data class SoundCapsuleWithSongs(
        val userId: Int,
        val soundCapsule: SoundCapsule? = null,
        val streakSongs: List<DayStreakSongWithDetails> = emptyList(),
        val topSong: TopSongDetails? = null,
        val topStreakSong: TopStreakSong? = null
    )
    
    data class DayStreakSongWithDetails(
        val songId: Int?,
        val name: String?,
        val artist: String?,
        val artwork: String?,
        val playDate: String,
        val playCount: Int
    )
    
    data class TopSongDetails(
        val id: Int,
        val name: String,
        val artist: String,
        val description: String,
        val duration: Long,
        val artwork: String?
    )
    
    @Transaction
    suspend fun getSoundCapsuleWithDetails(userId: Int, year: Int? = null, month: Int? = null): SoundCapsuleWithSongs {
        // Get Sound Capsule
        val capsule = if (year != null && month != null) {
            getSoundCapsuleByMonth(userId, year, month)
        } else {
            getLatestSoundCapsule(userId)
        }
        
        val result = SoundCapsuleWithSongs(userId = userId, soundCapsule = capsule)
        
        if (capsule != null) {
            // Get Streak Songs
            val streakSongs = getDayStreakSongsByCapsuleId(capsule.id)
            val streakSongsWithDetails = streakSongs.map { streakSong ->
                if (streakSong.songId != null) {
                    val songDetails = getSongDetails(streakSong.songId)
                    DayStreakSongWithDetails(
                        songId = streakSong.songId,
                        name = songDetails?.name,
                        artist = songDetails?.artist,
                        artwork = songDetails?.artwork,
                        playDate = streakSong.date,
                        playCount = streakSong.playCount
                    )
                } else {
                    DayStreakSongWithDetails(
                        songId = null,
                        name = null,
                        artist = null,
                        artwork = null,
                        playDate = streakSong.date,
                        playCount = streakSong.playCount
                    )
                }
            }
            
            // Get Top Song Details
            val topSong = if (capsule.topSongId != null) {
                getSongDetails(capsule.topSongId)
            } else {
                null
            }
            
            // Get Top Streak Song
            val topStreakSong = getTopStreakSongForDisplay(capsule.id)
            
            return SoundCapsuleWithSongs(
                userId = userId,
                soundCapsule = capsule,
                streakSongs = streakSongsWithDetails,
                topSong = topSong,
                topStreakSong = topStreakSong
            )
        }
        
        return result
    }
    
    @Query("""
        SELECT s.id, s.name, s.artist, s.description, s.duration, s.artwork
        FROM songs s
        WHERE s.id = :songId
    """)
    suspend fun getSongDetails(songId: Int): TopSongDetails?

    @Query("""
        SELECT SUM(duration)
        FROM listening_activity
        WHERE userId = :userId
        AND strftime('%Y-%m', startTime / 1000, 'unixepoch', 'localtime') = strftime('%Y-%m', 'now', 'localtime')
    """)
    suspend fun getTotalListeningTimeThisMonth(userId: Int): Long

    @Query("""
        SELECT s.artist, COUNT(la.songId) AS playCount
        FROM listening_activity la
        JOIN songs s ON la.songId = s.id
        WHERE la.userId = :userId
        AND strftime('%Y-%m', la.startTime / 1000, 'unixepoch', 'localtime') = strftime('%Y-%m', 'now', 'localtime')
        AND la.completed = 1
        GROUP BY s.artist
        ORDER BY playCount DESC
        LIMIT 1
    """)
    suspend fun getTopArtistThisMonth(userId: Int): TopArtistResult?

    data class TopArtistResult(
        val artist: String,
        val playCount: Int
    )

    @Query("""
        SELECT s.id, s.name, s.artist, s.description, s.duration, s.artwork, COUNT(la.songId) AS playCount
        FROM listening_activity la
        JOIN songs s ON la.songId = s.id
        WHERE la.userId = :userId
        AND strftime('%Y-%m', la.startTime / 1000, 'unixepoch', 'localtime') = strftime('%Y-%m', 'now', 'localtime')
        AND la.completed = 1
        GROUP BY s.id
        ORDER BY playCount DESC
        LIMIT 1
    """)
    suspend fun getTopSongThisMonth(userId: Int): TopSongDetails?

    @Query("""
        SELECT 
            la.songId as songId,
            COALESCE(s.name, la.songName) as name,
            COALESCE(s.artist, la.songArtist) as artist,
            s.artwork as artwork,
            DATE(la.startTime / 1000, 'unixepoch', 'localtime') as playDate,
            COUNT(*) as playCount
        FROM listening_activity la
        LEFT JOIN songs s ON la.songId = s.id
        WHERE la.userId = :userId
        AND la.completed = 1
        AND DATE(la.startTime / 1000, 'unixepoch', 'localtime') >= DATE('now', 'localtime', '-30 days')
        GROUP BY songId, name, artist, artwork, playDate
        ORDER BY playDate DESC
    """)
    suspend fun getPlayedSongsForDayStreak(userId: Int): List<DayStreakSongData>

    data class DayStreakSongData(
        val songId: Int?,
        val name: String?,
        val artist: String?,
        val artwork: String?,
        val playDate: String,
        val playCount: Int
    )

    // Calculate day streak from played songs data
    private fun calculateDayStreak(streakSongs: List<DayStreakSongData>): Int {
        if (streakSongs.isEmpty()) return 0
        
        // Group songs by date to check if we have any plays on each date
        val songsByDate = streakSongs.groupBy { it.playDate }
        val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        
        // Sort dates in descending order (newest first)
        val sortedDates = songsByDate.keys
            .map { LocalDate.parse(it, dateFormatter) }
            .sortedDescending()
            
        if (sortedDates.isEmpty()) return 0
        
        // Check if today has plays
        val today = LocalDate.now()
        if (sortedDates.first() != today) return 0
        
        // Start with today and go backwards
        var currentDate = today
        var consecutiveDays = 1  // Start with 1 for today
        
        for (i in 1 until 30) {  // Check up to 30 days back
            val checkDate = today.minusDays(i.toLong())
            val checkDateStr = checkDate.format(dateFormatter)
            
            // If we have songs for this date, continue the streak
            if (songsByDate.containsKey(checkDateStr)) {
                consecutiveDays++
            } else {
                // Break on the first day without plays
                break
            }
        }
        
        return consecutiveDays
    }
    
    @Transaction
    suspend fun generateAndSaveSoundCapsule(
        userId: Int,
        listeningActivityDao: ListeningActivityDao? = null,
        forceRegenerateStreakSongs: Boolean = false
    ): SoundCapsuleWithSongs {
        val currentDate = LocalDate.now()
        val year = currentDate.year
        val month = currentDate.monthValue
        
        // Check if we already have a Sound Capsule for this month
        var existingSoundCapsule = getSoundCapsuleByMonth(userId, year, month)
        
        // Generate new data directly
        val totalTimeListened = getTotalListeningTimeThisMonth(userId)
        val topArtist = getTopArtistThisMonth(userId)
        val topSong = getTopSongThisMonth(userId)
        val streakSongs = getPlayedSongsForDayStreak(userId)
        val dayStreak = calculateDayStreak(streakSongs)
        
        if (existingSoundCapsule == null) {
            // Create new SoundCapsule
            val newCapsule = SoundCapsule(
                userId = userId,
                year = year,
                month = month,
                totalTimeListened = totalTimeListened,
                topArtistName = topArtist?.artist,
                topArtistPlayCount = topArtist?.playCount,
                topSongId = topSong?.id,
                listeningDayStreak = dayStreak
            )
            
            val capsuleId = insertSoundCapsule(newCapsule)
            existingSoundCapsule = getSoundCapsuleByMonth(userId, year, month)
            
            // Save streak songs
            if (existingSoundCapsule != null) {
                saveDayStreakSongs(existingSoundCapsule.id, streakSongs)
            }
        } else {
            // Update existing SoundCapsule
            val updatedCapsule = existingSoundCapsule.copy(
                totalTimeListened = totalTimeListened,
                topArtistName = topArtist?.artist,
                topArtistPlayCount = topArtist?.playCount,
                topSongId = topSong?.id,
                listeningDayStreak = dayStreak,
                lastUpdated = System.currentTimeMillis()
            )
            updateSoundCapsule(updatedCapsule)
            
            // Update streak songs if requested
            if (forceRegenerateStreakSongs) {
                saveDayStreakSongs(existingSoundCapsule.id, streakSongs)
            }
        }
        
        // Return the complete data
        return getSoundCapsuleWithDetails(userId, year, month)
    }
    
    private suspend fun saveDayStreakSongs(
        capsuleId: Int,
        streakSongs: List<DayStreakSongData>
    ) {
        val dayStreakEntities = streakSongs.map { streakSong ->
            DayStreakSong(
                capsuleId = capsuleId,
                songId = streakSong.songId,
                date = streakSong.playDate,
                playCount = streakSong.playCount
            )
        }
        insertAllDayStreakSongs(dayStreakEntities)
    }
    
    @Query("""
        SELECT s.* 
        FROM sound_capsules s
        WHERE s.userId = :userId
        AND s.lastUpdated < :cutoffTime
    """)
    suspend fun getOutdatedSoundCapsules(userId: Int, cutoffTime: Long): List<SoundCapsule>

    @Transaction
    suspend fun updateSoundCapsuleInRealTime(
        userId: Int,
        songId: Int,
        duration: Long,
        isComplete: Boolean
    ) {
        val currentDate = LocalDate.now()
        val year = currentDate.year
        val month = currentDate.monthValue
        
        // Get or create Sound Capsule for current month
        var soundCapsule = getSoundCapsuleByMonth(userId, year, month)
        
        if (soundCapsule == null) {
            // No Sound Capsule for this month, create a new one
            // Use the full generation method to ensure all data is properly initialized
            generateAndSaveSoundCapsule(userId, null)
            soundCapsule = getSoundCapsuleByMonth(userId, year, month)
            
            // If still null after generation attempt, create a minimal one
            if (soundCapsule == null) {
                val newCapsule = SoundCapsule(
                    userId = userId,
                    year = year,
                    month = month,
                    totalTimeListened = duration,
                    topArtistName = null,
                    topArtistPlayCount = null,
                    topSongId = null,
                    listeningDayStreak = 0,
                    lastUpdated = System.currentTimeMillis()
                )
                val capsuleId = insertSoundCapsule(newCapsule)
                soundCapsule = getSoundCapsuleByMonth(userId, year, month)
            }
        } else if (duration > 0) {
            // Only update the listening time if there's a positive duration to add
            // This prevents double-counting when we're just updating completion status
            val updatedCapsule = soundCapsule.copy(
                totalTimeListened = soundCapsule.totalTimeListened + duration,
                lastUpdated = System.currentTimeMillis()
            )
            updateSoundCapsule(updatedCapsule)
        }
        
        // If song completed, add it to the day streak songs for today
        if (isComplete && soundCapsule != null) {
            addCompletedSongToStreak(soundCapsule.id, songId)
        }
        
        // Always refresh the top songs and artists data on completed songs,
        // or periodically even for incomplete songs to keep the UI fresh
        if (isComplete || shouldRefreshTopData(soundCapsule)) {
            updateTopSongAndArtistData(userId, soundCapsule?.id)
        }
    }

    /**
     * Determines if we should refresh top songs/artists data
     * This ensures the Sound Capsule UI stays current without excessive database operations
     */
    private fun shouldRefreshTopData(soundCapsule: SoundCapsule?): Boolean {
        if (soundCapsule == null) return true
        
        // Refresh if:
        // 1. We don't have top song/artist data
        // 2. It's been more than 5 minutes since the last update
        val noTopData = soundCapsule.topSongId == null || soundCapsule.topArtistName == null
        val lastUpdateTime = soundCapsule.lastUpdated
        val currentTime = System.currentTimeMillis()
        val fiveMinutesMs = 5 * 60 * 1000
        
        return noTopData || (currentTime - lastUpdateTime > fiveMinutesMs)
    }

    /**
     * Updates top song and artist data directly from listening activity data
     */
    private suspend fun updateTopSongAndArtistData(userId: Int, capsuleId: Int?) {
        if (capsuleId == null) return
        
        val soundCapsule = getSoundCapsuleById(capsuleId) ?: return
        
        // Get current top song and artist from the database
        val topSong = getTopSongThisMonth(userId)
        val topArtist = getTopArtistThisMonth(userId)
        
        // Only update the database if the data has changed
        if (soundCapsule.topSongId != topSong?.id || 
            soundCapsule.topArtistName != topArtist?.artist ||
            soundCapsule.topArtistPlayCount != topArtist?.playCount) {
            
            val updatedCapsule = soundCapsule.copy(
                topSongId = topSong?.id,
                topArtistName = topArtist?.artist,
                topArtistPlayCount = topArtist?.playCount,
                lastUpdated = System.currentTimeMillis()
            )
            updateSoundCapsule(updatedCapsule)
        }
    }

    private suspend fun addCompletedSongToStreak(capsuleId: Int, songId: Int) {
        val today = LocalDate.now().toString()
        
        // Check if we already have an entry for this song today
        val existingSongs = getDayStreakSongsForDateAndSong(capsuleId, today, songId)
        
        if (existingSongs.isNotEmpty()) {
            // Update the play count for the existing song
            val existingSong = existingSongs.first()
            val updatedSong = existingSong.copy(
                playCount = existingSong.playCount + 1
            )
            updateDayStreakSong(updatedSong)
        } else {
            // Create a new record for this song
            val newStreakSong = DayStreakSong(
                capsuleId = capsuleId,
                songId = songId,
                date = today,
                playCount = 1
            )
            insertDayStreakSong(newStreakSong)
        }
        
        // Update the listening day streak count
        updateListeningDayStreak(capsuleId)
    }

    @Query("SELECT * FROM day_streak_songs WHERE capsuleId = :capsuleId AND date = :date AND songId = :songId")
    suspend fun getDayStreakSongsForDateAndSong(capsuleId: Int, date: String, songId: Int): List<DayStreakSong>

    @Query("SELECT * FROM day_streak_songs WHERE capsuleId = :capsuleId AND date = :date")
    suspend fun getDayStreakSongsForDate(capsuleId: Int, date: String): List<DayStreakSong>

    @Update
    suspend fun updateDayStreakSong(dayStreakSong: DayStreakSong)

    private suspend fun updateListeningDayStreak(capsuleId: Int) {
        val soundCapsule = getSoundCapsuleById(capsuleId) ?: return
        
        // Get all streak songs and group by date
        val allStreakSongs = getDayStreakSongsByCapsuleId(capsuleId)
        val songsGroupedByDate = allStreakSongs.groupBy { it.date }
        
        // Convert to the format needed for streak calculation
        val streakSongsData = songsGroupedByDate.map { (date, songs) ->
            // For each date, take the first song as representative
            val firstSong = songs.firstOrNull()
            DayStreakSongData(
                songId = firstSong?.songId,
                name = null,
                artist = null,
                artwork = null,
                playDate = date,
                playCount = songs.sumOf { it.playCount } // Total plays on this date
            )
        }.sortedByDescending { it.playDate }
        
        val dayStreak = calculateDayStreak(streakSongsData)
        
        // Update the Sound Capsule with the new streak count
        val updatedCapsule = soundCapsule.copy(
            listeningDayStreak = dayStreak,
            lastUpdated = System.currentTimeMillis()
        )
        updateSoundCapsule(updatedCapsule)
    }

    @Query("SELECT * FROM sound_capsules WHERE id = :id")
    suspend fun getSoundCapsuleById(id: Int): SoundCapsule?

    fun observeSoundCapsuleWithDetails(userId: Int): Flow<SoundCapsuleWithSongs> = flow {
        // Initial emission
        val initialData = getSoundCapsuleWithDetails(userId)
        emit(initialData)
        
        // Get the latest Sound Capsule
        val latestCapsule = getLatestSoundCapsule(userId)
        
        if (latestCapsule != null) {
            // Create a merged flow that triggers when either the Sound Capsule or day streak songs change
            val capsuleFlow = getAllSoundCapsules(userId)
            val streakSongsFlow = observeDayStreakSongs(latestCapsule.id)
            
            // Merge both flows to trigger updates when either changes
            merge(capsuleFlow.map { "capsule" }, streakSongsFlow.map { "streak" }).collect {
                // When either data source changes, get the full updated data
                val updatedData = getSoundCapsuleWithDetails(userId)
                emit(updatedData)
            }
        } else {
            // If no Sound Capsule exists yet, just observe the Sound Capsule table
            getAllSoundCapsules(userId).collect { capsules ->
                if (capsules.isNotEmpty()) {
                    val updatedData = getSoundCapsuleWithDetails(userId)
                    emit(updatedData)
                }
            }
        }
    }
} 