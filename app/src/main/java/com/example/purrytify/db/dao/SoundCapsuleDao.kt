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
    
    data class SoundCapsuleWithSongs(
        val userId: Int,
        val soundCapsule: SoundCapsule? = null,
        val streakSongs: List<DayStreakSongWithDetails> = emptyList(),
        val topSong: TopSongDetails? = null
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
            
            return SoundCapsuleWithSongs(
                userId = userId,
                soundCapsule = capsule,
                streakSongs = streakSongsWithDetails,
                topSong = topSong
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
        WITH RECURSIVE dates(date) AS (
            SELECT DATE('now', 'localtime')
            UNION ALL
            SELECT DATE(date, '-1 day')
            FROM dates
            WHERE date > DATE('now', 'localtime', '-30 days')
        ),
        daily_plays AS (
            SELECT 
                s.id as songId,
                s.name,
                s.artist,
                s.artwork,
                DATE(la.startTime / 1000, 'unixepoch', 'localtime') as playDate,
                COUNT(*) as playCount
            FROM listening_activity la
            JOIN songs s ON la.songId = s.id
            WHERE la.userId = :userId
            AND la.completed = 1
            AND DATE(la.startTime / 1000, 'unixepoch', 'localtime') >= DATE('now', 'localtime', '-30 days')
            GROUP BY s.id, s.name, s.artist, s.artwork, playDate
        )
        SELECT 
            daily_plays.songId as songId,
            daily_plays.name as name,
            daily_plays.artist as artist,
            daily_plays.artwork as artwork,
            dates.date as playDate,
            COALESCE(daily_plays.playCount, 0) as playCount
        FROM dates
        LEFT JOIN daily_plays ON dates.date = daily_plays.playDate
        ORDER BY dates.date DESC
    """)
    suspend fun getDayStreakSongs(userId: Int): List<DayStreakSongData>

    data class DayStreakSongData(
        val songId: Int?,
        val name: String?,
        val artist: String?,
        val artwork: String?,
        val playDate: String,
        val playCount: Int
    )

    // Calculate day streak from streak songs
    private fun calculateDayStreak(streakSongs: List<DayStreakSongData>): Int {
        if (streakSongs.isEmpty()) return 0

        var currentStreak = 0
        var currentDate = LocalDate.now()

        for (song in streakSongs) {
            val songDate = LocalDate.parse(song.playDate)
            if (songDate == currentDate && song.playCount > 0) {
                currentStreak++
                currentDate = currentDate.minusDays(1)
            } else if (song.playCount == 0) {
                break
            }
        }

        return currentStreak
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
        val streakSongs = getDayStreakSongs(userId)
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
} 