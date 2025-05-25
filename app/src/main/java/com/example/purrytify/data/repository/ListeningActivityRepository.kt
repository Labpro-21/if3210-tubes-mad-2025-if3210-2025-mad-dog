package com.example.purrytify.data.repository

import com.example.purrytify.db.dao.ListeningActivityDao
import com.example.purrytify.db.dao.ListeningActivityDao.MonthlyPlayedSong
import com.example.purrytify.db.entity.ListeningActivity

class ListeningActivityRepository private constructor(
    private val listeningActivityDao: ListeningActivityDao
) {

    companion object {
        @Volatile
        private var instance: ListeningActivityRepository? = null

        fun getInstance(listeningActivityDao: ListeningActivityDao): ListeningActivityRepository {
            return instance ?: synchronized(this) {
                instance ?: ListeningActivityRepository(listeningActivityDao).also { instance = it }
            }
        }
    }

    suspend fun insert(listeningActivity: ListeningActivity): Long {
        return listeningActivityDao.insert(listeningActivity)
    }

    suspend fun update(listeningActivity: ListeningActivity) {
        listeningActivityDao.update(listeningActivity)
    }

    suspend fun delete(listeningActivity: ListeningActivity) {
        listeningActivityDao.delete(listeningActivity)
    }

    suspend fun deleteById(id: Int) {
        listeningActivityDao.deleteById(id)
    }

    suspend fun getById(id: Int): ListeningActivity? {
        return listeningActivityDao.getById(id)
    }


    suspend fun getDailyListeningData(userId: Int): List<ListeningActivityDao.DailyListeningStats>{
        return listeningActivityDao.getDailyListeningStatsLastMonth(userId)
    }

    suspend fun getMonthlyPlayedSongs(userId: Int): List<ListeningActivityDao.MonthlyPlayedSong>{
        return listeningActivityDao.getMonthlyPlayedSongs(userId)
    }

    suspend fun getMonthlyArtistsStats(userId: Int): List<ListeningActivityDao.MonthlyArtistStats> {
        return listeningActivityDao.getMonthlyArtistsStats(userId)
    }

    suspend fun getDailyListeningData(userId: Int, year: Int? = null, month: Int? = null): List<ListeningActivityDao.DailyListeningStats>{
        return if (year != null && month != null) {
            listeningActivityDao.getDailyListeningStatsByMonth(userId, year, month)
        } else {
            listeningActivityDao.getDailyListeningStatsLastMonth(userId)
        }
    }

    suspend fun getMonthlyPlayedSongs(userId: Int, year: Int? = null, month: Int? = null): List<ListeningActivityDao.MonthlyPlayedSong>{
        return if (year != null && month != null) {
            listeningActivityDao.getMonthlyPlayedSongsByMonth(userId, year, month)
        } else {
            listeningActivityDao.getMonthlyPlayedSongs(userId)
        }
    }

    suspend fun getMonthlyArtistsStats(userId: Int, year: Int? = null, month: Int? = null): List<ListeningActivityDao.MonthlyArtistStats> {
        return if (year != null && month != null) {
            listeningActivityDao.getMonthlyArtistsStatsByMonth(userId, year, month)
        } else {
            listeningActivityDao.getMonthlyArtistsStats(userId)
        }
    }
}