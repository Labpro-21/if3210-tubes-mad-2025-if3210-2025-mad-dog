package com.example.purrytify.data.auth

import com.example.purrytify.data.api.ProfileApi
import com.example.purrytify.data.model.ProfileResponse
import com.example.purrytify.db.dao.SongsDao
import retrofit2.Response

class ProfileRepository private constructor(
    private val api: ProfileApi,
    private val tokenManager: TokenManager,
    private val dao: SongsDao
){
    suspend fun getProfile():Response<ProfileResponse>?{
        val token = tokenManager.getAccessToken();
        return  if (token!=null){
            api.getProfile("Bearer $token");
        }else{
            null;
        }
    };

    suspend fun getSongsCount():Int{
        return dao.getSongsAmount();
    };

    suspend fun getSongsLiked():Int{
        return dao.getFavoriteSongsAmount();
    }
}