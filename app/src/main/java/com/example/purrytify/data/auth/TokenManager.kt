package com.example.purrytify.data.auth

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import javax.crypto.AEADBadTagException

class TokenManager private constructor(private val context: Context) {

    private object PreferencesKeys {
        const val ACCESS_TOKEN = "access_token"
        const val REFRESH_TOKEN = "refresh_token"
        const val PREFS_FILENAME = "encrypted_auth_prefs"
    }
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedSharedPreferences by lazy {
        try {
            createEncryptedPreferences()
        } catch (e: Exception) {
            Log.e(TAG, "Error creating encrypted preferences: ${e.message}", e)
            
            // If we get a bad tag exception, it means our encryption is corrupted
            // Clear the old preferences file and create a new one
            if (e.cause is AEADBadTagException || e is AEADBadTagException) {
                Log.w(TAG, "Encryption corrupted, clearing preferences and creating new ones")
                clearCorruptedPreferences()
                createEncryptedPreferences()
            } else {
                // For other exceptions, rethrow
                throw e
            }
        }
    }
    
    private fun createEncryptedPreferences() = EncryptedSharedPreferences.create(
        context,
        PreferencesKeys.PREFS_FILENAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    private fun clearCorruptedPreferences() {
        // Delete the corrupted SharedPreferences file
        context.deleteSharedPreferences(PreferencesKeys.PREFS_FILENAME)
        
        // Clear our singleton instance to force recreation
        synchronized(Companion::class.java) {
            INSTANCE = null
        }
    }
    
    val accessTokenFlow: Flow<String?> = flow {
        try {
            emit(encryptedSharedPreferences.getString(PreferencesKeys.ACCESS_TOKEN, null))
        } catch (e: Exception) {
            Log.e(TAG, "Error reading access token: ${e.message}", e)
            emit(null)
        }
    }

    val refreshTokenFlow: Flow<String?> = flow {
        try {
            emit(encryptedSharedPreferences.getString(PreferencesKeys.REFRESH_TOKEN, null))
        } catch (e: Exception) {
            Log.e(TAG, "Error reading refresh token: ${e.message}", e)
            emit(null)
        }
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
        try {
            encryptedSharedPreferences.edit()
                .putString(PreferencesKeys.ACCESS_TOKEN, accessToken)
                .putString(PreferencesKeys.REFRESH_TOKEN, refreshToken)
                .apply()
                
            _tokenFlow.value = accessToken
            Log.d(TAG, "Tokens saved to EncryptedSharedPreferences")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving tokens: ${e.message}", e)
        }
    }

    suspend fun clearTokens() {
        try {
            encryptedSharedPreferences.edit()
                .remove(PreferencesKeys.ACCESS_TOKEN)
                .remove(PreferencesKeys.REFRESH_TOKEN)
                .apply()
                
            _tokenFlow.value = null
            Log.d(TAG, "Tokens cleared from EncryptedSharedPreferences")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing tokens: ${e.message}", e)
        }
    }

    suspend fun getAccessToken(): String? {
        return try {
            encryptedSharedPreferences.getString(PreferencesKeys.ACCESS_TOKEN, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting access token: ${e.message}", e)
            null
        }
    }
    
    suspend fun getRefreshToken(): String? {
        return try {
            encryptedSharedPreferences.getString(PreferencesKeys.REFRESH_TOKEN, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting refresh token: ${e.message}", e)
            null
        }
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