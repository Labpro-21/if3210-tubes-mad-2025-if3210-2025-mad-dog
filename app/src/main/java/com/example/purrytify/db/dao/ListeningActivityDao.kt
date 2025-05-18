package com.example.purrytify.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.purrytify.db.entity.ListeningActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Dao
interface ListeningActivityDao {

    @Insert
    suspend fun insert(listeningActivity: ListeningActivity): Long // Mengembalikan ID yang baru diinsert

    @Update
    suspend fun update(listeningActivity: ListeningActivity)

    @Delete
    suspend fun delete(listeningActivity: ListeningActivity)

    @Query("DELETE FROM listening_activity WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM listening_activity WHERE id = :id")
    suspend fun getById(id: Int): ListeningActivity?

    @Query("SELECT * FROM listening_activity WHERE userId = :userId ORDER BY startTime DESC")
    suspend fun getAllByUserId(userId: Int): List<ListeningActivity> // Fungsi baru

    @Transaction
    @Query(
        """
        SELECT SUM(duration)
        FROM listening_activity
        WHERE userId = :userId
        AND strftime('%Y-%m', startTime / 1000, 'unixepoch', 'localtime') = strftime('%Y-%m', 'now', 'localtime')
        
        """
    )
    //AND completed = 1
    fun getTotalListeningTimeThisMonth(userId: Int): Long

    // Changed return type to TopArtist data class instead of String
    @Query(
        """
        SELECT s.artist, COUNT(la.songId) AS playCount
        FROM listening_activity la
        JOIN songs s ON la.songId = s.id
        WHERE la.userId = :userId
        AND strftime('%Y-%m', la.startTime / 1000, 'unixepoch', 'localtime') = strftime('%Y-%m', 'now', 'localtime')
        AND la.completed = 1
        GROUP BY s.artist
        ORDER BY playCount DESC
        LIMIT 1
        """
    )
    fun getTopArtistThisMonth(userId: Int): TopArtist?

    // Modified to return more complete song information
    @Query(
        """
        SELECT s.id, s.name, s.artist, s.description, s.duration, s.artwork, COUNT(la.songId) AS playCount
        FROM listening_activity la
        JOIN songs s ON la.songId = s.id
        WHERE la.userId = :userId
        AND strftime('%Y-%m', la.startTime / 1000, 'unixepoch', 'localtime') = strftime('%Y-%m', 'now', 'localtime')
        AND la.completed = 1
        GROUP BY s.id
        ORDER BY playCount DESC
        LIMIT 1
        """
    )
    fun getTopSongThisMonth(userId: Int): TopSongComplete?

    @Query(
        """
        SELECT la.songId, DATE(la.startTime / 1000, 'unixepoch', 'localtime') AS playDate, COUNT(*) AS playCount
        FROM listening_activity la
        WHERE la.userId = :userId
        AND la.completed = 1
        AND DATE(la.startTime / 1000, 'unixepoch', 'localtime') >= DATE('now', 'localtime', '-2 days')
        GROUP BY la.songId, playDate
        HAVING COUNT(*) >= 1
        ORDER BY playDate DESC
    """
    )
    fun getRecentListeningActivity(userId: Int): List<RecentPlayRecord>

    // Data class for query results
    data class TopArtist(
        val artist: String,
        val playCount: Int
    )

    // New data class with complete song information
    data class TopSongComplete(
        val id: Int,
        val name: String,
        val artist: String,
        val description: String,
        val duration: Long,
        val artwork: String?,
        val playCount: Int
    )

    // Data class untuk menampung hasil kueri day-streak
    data class RecentPlayRecord(
        val songId: Int,
        val playDate: String,
        val playCount: Int
    )

    // Fungsi untuk menghitung day-streak (membutuhkan pemrosesan lebih lanjut di luar kueri)
    fun calculateDayStreak(userId: Int): Int {
        val recentPlays = getRecentListeningActivity(userId)
        if (recentPlays.isEmpty()) {
            return 0
        }

        val playedDates = recentPlays.groupBy { it.songId }.mapValues { entry ->
            entry.value.map { it.playDate }.toSet()
        }

        var maxStreak = 0
        for ((_, dates) in playedDates) {
            var currentStreak = 0
            var currentDate = java.time.LocalDate.now()
            while (dates.contains(currentDate.toString())) {
                currentStreak++
                currentDate = currentDate.minusDays(1)
            }
            if (currentStreak > maxStreak) {
                maxStreak = currentStreak
            }
        }
        return maxStreak
    }

    // Updated function to get all Sound Capsule data with complete song information and month-year
    @Transaction
    fun getSoundCapsuleData(userId: Int): SoundCapsule {
        val totalTime = getTotalListeningTimeThisMonth(userId)
        val topArtistResult = getTopArtistThisMonth(userId)
        val topSongResult = getTopSongThisMonth(userId)
        val dayStreak = calculateDayStreak(userId)

        val currentMonthYear = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))

        return SoundCapsule(
            totalTimeListened = totalTime,
            topArtist = topArtistResult?.artist,
            topSong = topSongResult, // Now passing the entire TopSongComplete object
            listeningDayStreak = dayStreak,
            monthYear = currentMonthYear // Added month and year information
        )
    }

    // Updated data class for Sound Capsule with complete song information and month-year
    data class SoundCapsule(
        val totalTimeListened: Long,
        val topArtist: String?,
        val topSong: TopSongComplete?, // Changed from String? to TopSongComplete?
        val listeningDayStreak: Int,
        val monthYear: String // Added property for month and year
    )

    // Data class to hold daily listening statistics
    data class DailyListeningStats(
        val date: String,  // Format: YYYY-MM-DD
        val totalMinutes: Double,  // Duration in minutes
    )

    @Query("""
        SELECT 
            DATE(startTime / 1000, 'unixepoch', 'localtime') as date,
            ROUND(CAST(SUM(duration) AS FLOAT) / 60000.0, 2) as totalMinutes
        FROM listening_activity
        WHERE userId = :userId
        
        AND startTime >= strftime('%s000', 'now', '-30 days')
        AND startTime <= strftime('%s000', 'now')
        GROUP BY DATE(startTime / 1000, 'unixepoch', 'localtime')
        ORDER BY date ASC
    """)
    //AND completed = 1
    suspend fun getDailyListeningStatsLastMonth(userId: Int): List<DailyListeningStats>

    // Data class for monthly played songs
    data class MonthlyPlayedSong(
        val name: String,
        val artist: String,
        val artwork: String?,
        val playCount: Int
    )

    @Query(
        """
        SELECT s.name, s.artist, s.artwork, COUNT(la.songId) AS playCount
        FROM listening_activity la
        JOIN songs s ON la.songId = s.id
        WHERE la.userId = :userId
        AND strftime('%Y-%m', la.startTime / 1000, 'unixepoch', 'localtime') = strftime('%Y-%m', 'now', 'localtime')
        AND la.completed = 1
        GROUP BY s.id, s.name, s.artist, s.artwork
        ORDER BY playCount DESC
        """
    )
    suspend fun getMonthlyPlayedSongs(userId: Int): List<MonthlyPlayedSong>

    // Data class for monthly artist statistics
    data class MonthlyArtistStats(
        val artist: String,
        val playCount: Int,
        val artwork: String?  // Random artwork from one of the artist's songs
    )

    @Query(
        """
        SELECT 
            s.artist,
            COUNT(DISTINCT la.id) as playCount,
            (
                SELECT artwork 
                FROM songs s2 
                WHERE s2.artist = s.artist 
                LIMIT 1
            ) as artwork
        FROM listening_activity la
        JOIN songs s ON la.songId = s.id
        WHERE la.userId = :userId
        AND strftime('%Y-%m', la.startTime / 1000, 'unixepoch', 'localtime') = strftime('%Y-%m', 'now', 'localtime')
        AND la.completed = 1
        GROUP BY s.artist
        ORDER BY playCount DESC
        """
    )
    suspend fun getMonthlyArtistsStats(userId: Int): List<MonthlyArtistStats>
}