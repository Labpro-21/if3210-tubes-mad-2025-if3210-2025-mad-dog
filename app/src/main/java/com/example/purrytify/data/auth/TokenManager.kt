package com.example.purrytify.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class TokenManager(private val context: Context) {
    
    companion object {
        private val JWT_TOKEN_KEY = stringPreferencesKey("jwt_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    }
    
    // Get the JWT token
    val tokenFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[JWT_TOKEN_KEY]
        }
    
    // Get the refresh token
    val refreshTokenFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[REFRESH_TOKEN_KEY]
        }
    
    // Save tokens to DataStore
    suspend fun saveTokens(jwtToken: String, refreshToken: String) {
        context.dataStore.edit { preferences ->
            preferences[JWT_TOKEN_KEY] = jwtToken
            preferences[REFRESH_TOKEN_KEY] = refreshToken
        }
    }
    
    // Clear all tokens (for logout)
    suspend fun clearTokens() {
        context.dataStore.edit { preferences ->
            preferences.remove(JWT_TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
        }
    }
}