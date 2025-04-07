package com.example.purrytify.data.auth

import android.app.Application
import com.example.purrytify.db.AppDatabase
import android.content.Context
import com.example.purrytify.data.api.NetworkModule
import com.example.purrytify.data.api.ProfileApi
import com.example.purrytify.data.model.ProfileResponse
import com.example.purrytify.db.dao.SongsDao
import retrofit2.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileRepository private constructor(
    private val tokenManager: TokenManager,
    private val dao: SongsDao
){
    private val api = NetworkModule.profileApi;

    suspend fun getProfile(): ProfileResponse? = withContext(Dispatchers.IO){
        val token = tokenManager.getAccessToken();
        if (token!=null){
            val response = api.getProfile("Bearer $token");
            if (response.isSuccessful){
                val profile = response.body();
                profile?.apply {
                    profilePhoto = NetworkModule.getProfileImageUrl(profilePhoto);
                }

                return@withContext profile;
            }
        }
        return@withContext null;
    };

    suspend fun getSongsCount():Int{
        return dao.getSongsAmount();
    };

    suspend fun getSongsLiked():Int{
        return dao.getFavoriteSongsAmount();
    }
    companion object {
        @Volatile
        private var INSTANCE: ProfileRepository? = null

        fun getInstance(application: Application): ProfileRepository {
            return INSTANCE ?: synchronized(this) {
                val context = application.applicationContext
                val tokenManager = TokenManager.getInstance(context)
                val dao = AppDatabase.getDatabase(context).songsDao()
                val instance = ProfileRepository(tokenManager, dao)
                INSTANCE = instance
                instance
            }
        }
    }
}