package com.example.purrytify.data.auth

import android.app.Application
import com.example.purrytify.db.AppDatabase
import android.content.Context
import com.example.purrytify.data.api.NetworkModule
import com.example.purrytify.data.api.ProfileApi
import com.example.purrytify.data.model.ProfileResponse
import com.example.purrytify.db.dao.SongsDao
import com.example.purrytify.db.dao.UsersDao
import retrofit2.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileRepository private constructor(
    private val tokenManager: TokenManager,
    private val songsdao: SongsDao,
    private val usersdao: UsersDao,
    private val authRepository: AuthRepository
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

    suspend fun getSongsCount():Int = withContext(Dispatchers.IO){
        return@withContext songsdao.getSongsAmount();
    };

    suspend fun getSongsLiked():Int = withContext(Dispatchers.IO) {
        return@withContext songsdao.getFavoriteSongsAmount();
    }
    suspend fun getTotalListened(): Int = withContext(Dispatchers.IO) {
        return@withContext usersdao.getTotalPlayedById(authRepository.currentUserId) ?: 0
    }

    companion object {
        @Volatile
        private var INSTANCE: ProfileRepository? = null

        fun getInstance(application: Application): ProfileRepository {
            return INSTANCE ?: synchronized(this) {
                val context = application.applicationContext
                val tokenManager = TokenManager.getInstance(context)
                val authRepository = AuthRepository.getInstance(application)
                val songsdao = AppDatabase.getDatabase(context).songsDao()
                val usersdao = AppDatabase.getDatabase(context).usersDao()
                val instance = ProfileRepository(tokenManager, songsdao= songsdao,usersdao=usersdao,authRepository= authRepository )
                INSTANCE = instance
                instance
            }
        }
    }
}