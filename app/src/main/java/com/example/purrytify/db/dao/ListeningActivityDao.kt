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

    data class DayStreakSong(
        val songId: Int?,
        val name: String?,
        val artist: String?,
        val artwork: String?,
        val playDate: String,
        val playCount: Int
    )

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
    suspend fun getDayStreakSongs(userId: Int): List<DayStreakSong>

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