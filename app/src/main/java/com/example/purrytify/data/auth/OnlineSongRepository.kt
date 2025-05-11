package com.example.purrytify.data.auth

import android.app.Application
import android.util.Log
import com.example.purrytify.data.api.NetworkModule
import com.example.purrytify.data.model.OnlineSongResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OnlineSongRepository private constructor(
    private val tokenManager: TokenManager,
) {
    private val api = NetworkModule.onlineSongApi
    private val tag = "OnlineSongRepository"

    suspend fun getTopGlobalSongs(): List<OnlineSongResponse>? = withContext(Dispatchers.IO) {
        val token = tokenManager.getAccessToken()
        Log.d(tag, "Fetching global songs with token: $token")
        if (token != null) {
            try {
                val response = api.getTopGlobalSongs("Bearer $token")
                Log.d(tag, "Global songs API response code: ${response.code()}")
                return@withContext handleResponse(response)
            } catch (e: Exception) {
                Log.e(tag, "Error fetching global songs: ${e.localizedMessage}", e)
            }
        } else {
            Log.d(tag, "Token is null, cannot fetch global songs")
        }
        return@withContext null
    }

    suspend fun getTopCountrySongs(countryCode: String): List<OnlineSongResponse>? = withContext(Dispatchers.IO) {
        val token = tokenManager.getAccessToken()
        Log.d(tag, "Fetching $countryCode songs with token: $token")
        if (token != null) {
            try {
                val response = api.getTopCountrySongs("Bearer $token", countryCode)
                Log.d(tag, "$countryCode songs API response code: ${response.code()}")
                return@withContext handleResponse(response)
            } catch (e: Exception) {
                Log.e(tag, "Error fetching $countryCode songs: ${e.localizedMessage}", e)
            }
        } else {
            Log.d(tag, "Token is null, cannot fetch $countryCode songs")
        }
        return@withContext null
    }

    private fun handleResponse(response: retrofit2.Response<List<OnlineSongResponse>>): List<OnlineSongResponse>? {
        if (response.isSuccessful) {
            val songs = response.body() ?: emptyList()
            Log.d(tag, "Fetched ${songs.size} songs from server")
            return songs
        } else {
            Log.d(tag, "Unsuccessful response: ${response.errorBody()?.string()}")
        }
        return null
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