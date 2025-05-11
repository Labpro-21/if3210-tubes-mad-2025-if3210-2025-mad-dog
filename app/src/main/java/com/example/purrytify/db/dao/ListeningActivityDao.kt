package com.example.purrytify.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.purrytify.db.entity.ListeningActivity

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
        AND completed = 1
        """
    )
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

    // Changed return type to TopSong data class instead of String
    @Query(
        """
        SELECT s.name, COUNT(la.songId) AS playCount
        FROM listening_activity la
        JOIN songs s ON la.songId = s.id
        WHERE la.userId = :userId
        AND strftime('%Y-%m', la.startTime / 1000, 'unixepoch', 'localtime') = strftime('%Y-%m', 'now', 'localtime')
        AND la.completed = 1
        GROUP BY s.name
        ORDER BY playCount DESC
        LIMIT 1
        """
    )
    fun getTopSongThisMonth(userId: Int): TopSong?

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

    // Data class for query results
    data class TopSong(
        val name: String,
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

    // Fungsi untuk mendapatkan semua data Sound Capsule dalam satu objek
    @Transaction
    fun getSoundCapsuleData(userId: Int): SoundCapsule {
        val totalTime = getTotalListeningTimeThisMonth(userId)
        val topArtistResult = getTopArtistThisMonth(userId)
        val topSongResult = getTopSongThisMonth(userId)
        val dayStreak = calculateDayStreak(userId)

        return SoundCapsule(
            totalTimeListened = totalTime,
            topArtist = topArtistResult?.artist,
            topSong = topSongResult?.name,
            listeningDayStreak = dayStreak
        )
    }

    // Data class untuk menampung data Sound Capsule
    data class SoundCapsule(
        val totalTimeListened: Long,
        val topArtist: String?,
        val topSong: String?,
        val listeningDayStreak: Int
    )
}