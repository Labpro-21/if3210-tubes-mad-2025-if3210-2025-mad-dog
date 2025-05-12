package com.example.purrytify.data.repository

import com.example.purrytify.db.dao.ListeningActivityDao
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

    suspend fun getAllByUserId(userId: Int): List<ListeningActivity> {
        return listeningActivityDao.getAllByUserId(userId)
    }

    suspend fun getTotalListeningTimeThisMonth(userId: Int): Long {
        return listeningActivityDao.getTotalListeningTimeThisMonth(userId)
    }

    suspend fun getTopArtistThisMonth(userId: Int): ListeningActivityDao.TopArtist? {
        return listeningActivityDao.getTopArtistThisMonth(userId)
    }

    suspend fun getTopSongThisMonth(userId: Int): ListeningActivityDao.TopSong? {
        return listeningActivityDao.getTopSongThisMonth(userId)
    }

    suspend fun getRecentListeningActivity(userId: Int): List<ListeningActivityDao.RecentPlayRecord> {
        return listeningActivityDao.getRecentListeningActivity(userId)
    }

    suspend fun calculateDayStreak(userId: Int): Int {
        return listeningActivityDao.calculateDayStreak(userId)
    }

    suspend fun getSoundCapsuleData(userId: Int): ListeningActivityDao.SoundCapsule {
        return listeningActivityDao.getSoundCapsuleData(userId)
    }
}