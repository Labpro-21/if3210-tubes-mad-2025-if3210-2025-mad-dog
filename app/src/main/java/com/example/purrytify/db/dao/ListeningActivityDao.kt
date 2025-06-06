package com.example.purrytify.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.purrytify.db.entity.ListeningActivity
import java.time.LocalDate

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
        SELECT 
            COALESCE(s.artist, la.songArtist) as artist, 
            COUNT(la.id) AS playCount
        FROM listening_activity la
        LEFT JOIN songs s ON la.songId = s.id
        WHERE la.userId = :userId
        AND strftime('%Y-%m', la.startTime / 1000, 'unixepoch', 'localtime') = strftime('%Y-%m', 'now', 'localtime')
        AND la.completed = 1
        GROUP BY artist
        ORDER BY playCount DESC
        LIMIT 1
        """
    )
    fun getTopArtistThisMonth(userId: Int): TopArtist?

    // Modified to return more complete song information
    @Query(
        """
        SELECT 
            COALESCE(s.id, 0) as id, 
            COALESCE(s.name, la.songName) as name, 
            COALESCE(s.artist, la.songArtist) as artist, 
            COALESCE(s.description, '') as description, 
            COALESCE(s.duration, 0) as duration, 
            s.artwork, 
            COUNT(la.id) AS playCount
        FROM listening_activity la
        LEFT JOIN songs s ON la.songId = s.id
        WHERE la.userId = :userId
        AND strftime('%Y-%m', la.startTime / 1000, 'unixepoch', 'localtime') = strftime('%Y-%m', 'now', 'localtime')
        AND la.completed = 1
        GROUP BY name, artist
        ORDER BY playCount DESC
        LIMIT 1
        """
    )
    fun getTopSongThisMonth(userId: Int): TopSongComplete?

    @Query(
        """
        SELECT 
            la.songId, 
            DATE(la.startTime / 1000, 'unixepoch', 'localtime') AS playDate, 
            COUNT(*) AS playCount
        FROM listening_activity la
        WHERE la.userId = :userId
        AND la.completed = 1
        AND DATE(la.startTime / 1000, 'unixepoch', 'localtime') >= DATE('now', 'localtime', '-2 days')
        AND la.songId IS NOT NULL
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

    data class DayStreakSong(
        val songId: Int?,
        val name: String?,
        val artist: String?,
        val artwork: String?,
        val playDate: String,
        val playCount: Int
    )



    // Update calculateDayStreak to handle nullable fields
    fun calculateDayStreak(streakSongs: List<DayStreakSong>): Int {
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

    // Data class to hold daily listening statistics
    data class DailyListeningStats(
        val date: String,  // Format: YYYY-MM-DD
        val totalMinutes: Double,  // Duration in minutes
    )

    @Query("""
        SELECT 
            DATE(la.startTime / 1000, 'unixepoch', 'localtime') as date,
            ROUND(CAST(SUM(la.duration) AS FLOAT) / 60000.0, 2) as totalMinutes
        FROM listening_activity la
        WHERE la.userId = :userId
        AND la.startTime >= strftime('%s000', 'now', '-30 days')
        AND la.startTime <= strftime('%s000', 'now')
        GROUP BY DATE(la.startTime / 1000, 'unixepoch', 'localtime')
        ORDER BY date ASC
    """)
    suspend fun getDailyListeningStatsLastMonth(userId: Int): List<DailyListeningStats>

    @Query("""
        SELECT 
            DATE(la.startTime / 1000, 'unixepoch', 'localtime') as date,
            ROUND(CAST(SUM(la.duration) AS FLOAT) / 60000.0, 2) as totalMinutes
        FROM listening_activity la
        WHERE la.userId = :userId
        AND strftime('%Y', la.startTime / 1000, 'unixepoch', 'localtime') = :year
        AND strftime('%m', la.startTime / 1000, 'unixepoch', 'localtime') = :month
        GROUP BY DATE(la.startTime / 1000, 'unixepoch', 'localtime')
        ORDER BY date ASC
    """)
    suspend fun getDailyListeningStatsByYearMonth(userId: Int, year: String, month: String): List<DailyListeningStats>

    suspend fun getDailyListeningStatsByMonth(userId: Int, year: Int, month: Int): List<DailyListeningStats> {
        val yearStr = year.toString()
        val monthStr = month.toString().padStart(2, '0')
        return getDailyListeningStatsByYearMonth(userId, yearStr, monthStr)
    }

    // Data class for monthly played songs
    data class MonthlyPlayedSong(
        val name: String,
        val artist: String,
        val artwork: String?,
        val playCount: Int
    )

    @Query(
        """
        SELECT 
            COALESCE(s.name, la.songName) as name, 
            COALESCE(s.artist, la.songArtist) as artist, 
            s.artwork, 
            COUNT(la.id) AS playCount
        FROM listening_activity la
        LEFT JOIN songs s ON la.songId = s.id
        WHERE la.userId = :userId
        AND strftime('%Y-%m', la.startTime / 1000, 'unixepoch', 'localtime') = strftime('%Y-%m', 'now', 'localtime')
        AND la.completed = 1
        GROUP BY name, artist, s.artwork
        ORDER BY playCount DESC
        """
    )
    suspend fun getMonthlyPlayedSongs(userId: Int): List<MonthlyPlayedSong>

    @Query(
        """
        SELECT 
            COALESCE(s.name, la.songName) as name, 
            COALESCE(s.artist, la.songArtist) as artist, 
            s.artwork, 
            COUNT(la.id) AS playCount
        FROM listening_activity la
        LEFT JOIN songs s ON la.songId = s.id
        WHERE la.userId = :userId
        AND strftime('%Y', la.startTime / 1000, 'unixepoch', 'localtime') = :year
        AND strftime('%m', la.startTime / 1000, 'unixepoch', 'localtime') = :month
        AND la.completed = 1
        GROUP BY name, artist, s.artwork
        ORDER BY playCount DESC
        """
    )
    suspend fun getMonthlyPlayedSongsByYearMonth(userId: Int, year: String, month: String): List<MonthlyPlayedSong>

    suspend fun getMonthlyPlayedSongsByMonth(userId: Int, year: Int, month: Int): List<MonthlyPlayedSong> {
        val yearStr = year.toString()
        val monthStr = month.toString().padStart(2, '0')
        return getMonthlyPlayedSongsByYearMonth(userId, yearStr, monthStr)
    }

    // Data class for monthly artist statistics
    data class MonthlyArtistStats(
        val artist: String,
        val playCount: Int,
        val artwork: String?  // Random artwork from one of the artist's songs
    )

    @Query(
        """
        SELECT 
            COALESCE(s.artist, la.songArtist) as artist,
            COUNT(DISTINCT la.id) as playCount,
            MAX(s.artwork) as artwork
        FROM listening_activity la
        LEFT JOIN songs s ON la.songId = s.id
        WHERE la.userId = :userId
        AND strftime('%Y-%m', la.startTime / 1000, 'unixepoch', 'localtime') = strftime('%Y-%m', 'now', 'localtime')
        AND la.completed = 1
        GROUP BY artist
        ORDER BY playCount DESC
        """
    )
    suspend fun getMonthlyArtistsStats(userId: Int): List<MonthlyArtistStats>

    @Query(
        """
        SELECT 
            COALESCE(s.artist, la.songArtist) as artist,
            COUNT(DISTINCT la.id) as playCount,
            MAX(s.artwork) as artwork
        FROM listening_activity la
        LEFT JOIN songs s ON la.songId = s.id
        WHERE la.userId = :userId
        AND strftime('%Y', la.startTime / 1000, 'unixepoch', 'localtime') = :year
        AND strftime('%m', la.startTime / 1000, 'unixepoch', 'localtime') = :month
        AND la.completed = 1
        GROUP BY artist
        ORDER BY playCount DESC
        """
    )
    suspend fun getMonthlyArtistsStatsByYearMonth(userId: Int, year: String, month: String): List<MonthlyArtistStats>

    suspend fun getMonthlyArtistsStatsByMonth(userId: Int, year: Int, month: Int): List<MonthlyArtistStats> {
        val yearStr = year.toString()
        val monthStr = month.toString().padStart(2, '0')
        return getMonthlyArtistsStatsByYearMonth(userId, yearStr, monthStr)
    }
}