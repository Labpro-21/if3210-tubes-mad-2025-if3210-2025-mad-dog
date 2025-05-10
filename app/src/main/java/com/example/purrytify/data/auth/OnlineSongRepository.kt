package com.example.purrytify.data.auth

import android.app.Application
import android.util.Log
import com.example.purrytify.db.AppDatabase
import com.example.purrytify.data.api.NetworkModule
import com.example.purrytify.data.model.OnlineSongResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OnlineSongRepository private constructor(
    private val tokenManager: TokenManager,
) {
    private val api = NetworkModule.onlineSongApi

    suspend fun getOnlineSongResponses(): List<OnlineSongResponse>? = withContext(Dispatchers.IO) {
        val tag = "OnlineSongRepository"
        val token = tokenManager.getAccessToken()

        Log.d(tag, "Fetching token: $token")

        if (token != null) {
            try {
                val response = api.getTopGlobalSongs("Bearer $token")
                Log.d(tag, "API response code: ${response.code()}")

                if (response.isSuccessful) {
                    val songs = response.body() ?: emptyList()
                    Log.d(tag, "Fetched ${songs.size} songs from server")
                    return@withContext songs
                } else {
                    Log.d(tag, "Unsuccessful response: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error fetching songs: ${e.localizedMessage}", e)
            }
        } else {
            Log.d(tag, "Token is null, cannot fetch songs")
        }

        return@withContext null
    }

    companion object {
        @Volatile
        private var INSTANCE: OnlineSongRepository? = null

        fun getInstance(application: Application): OnlineSongRepository {
            return INSTANCE ?: synchronized(this) {
                val context = application.applicationContext
                val tokenManager = TokenManager.getInstance(context)
                val instance = OnlineSongRepository(tokenManager)
                INSTANCE = instance
                instance
            }
        }
    }
}
