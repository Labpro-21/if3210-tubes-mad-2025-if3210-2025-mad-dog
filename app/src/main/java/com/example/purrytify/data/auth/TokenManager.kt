package com.example.purrytify.data.auth

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class TokenManager private constructor(private val context: Context) {

    private object PreferencesKeys {
        const val ACCESS_TOKEN = "access_token"
        const val REFRESH_TOKEN = "refresh_token"
    }
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
        
    private val encryptedSharedPreferences = EncryptedSharedPreferences.create(
        context,
        "encrypted_auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    val accessTokenFlow: Flow<String?> = flow {
        emit(encryptedSharedPreferences.getString(PreferencesKeys.ACCESS_TOKEN, null))
    }

    val refreshTokenFlow: Flow<String?> = flow {
        emit(encryptedSharedPreferences.getString(PreferencesKeys.REFRESH_TOKEN, null))
    }

    private val _tokenFlow = MutableStateFlow<String?>(null)
    val tokenFlow: StateFlow<String?> = _tokenFlow

    init {
        try {
            _tokenFlow.value = encryptedSharedPreferences.getString(PreferencesKeys.ACCESS_TOKEN, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing token flow: ${e.message}", e)
        }
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        encryptedSharedPreferences.edit()
            .putString(PreferencesKeys.ACCESS_TOKEN, accessToken)
            .putString(PreferencesKeys.REFRESH_TOKEN, refreshToken)
            .apply()
            
        _tokenFlow.value = accessToken
        Log.d(TAG, "Tokens saved to EncryptedSharedPreferences")
    }

    suspend fun clearTokens() {
        encryptedSharedPreferences.edit()
            .remove(PreferencesKeys.ACCESS_TOKEN)
            .remove(PreferencesKeys.REFRESH_TOKEN)
            .apply()
            
        _tokenFlow.value = null
        Log.d(TAG, "Tokens cleared from EncryptedSharedPreferences")
    }

    suspend fun getAccessToken(): String? {
        return encryptedSharedPreferences.getString(PreferencesKeys.ACCESS_TOKEN, null)
    }
    
    suspend fun getRefreshToken(): String? {
        return encryptedSharedPreferences.getString(PreferencesKeys.REFRESH_TOKEN, null)
    }

    companion object {
        private const val TAG = "TokenManager"

        @Volatile
        private var INSTANCE: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TokenManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}