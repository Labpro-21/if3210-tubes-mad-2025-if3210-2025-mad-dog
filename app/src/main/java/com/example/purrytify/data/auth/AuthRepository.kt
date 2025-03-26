package com.example.purrytify.data.auth

import com.example.purrytify.data.api.NetworkModule
import com.example.purrytify.data.model.LoginRequest
import com.example.purrytify.data.model.RefreshTokenRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

class AuthRepository(private val tokenManager: TokenManager) {
    
    private val authApi = NetworkModule.authApi
    
    suspend fun login(email: String, password: String): Result<Boolean> {
        return try {
            val email = email
            val password = password
            
            val response = authApi.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    tokenManager.saveTokens(authResponse.accessToken, authResponse.refreshToken)
                    return Result.success(true)
                }
                Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Login failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun refreshToken(): Result<Boolean> {
        return try {
            val refreshToken = tokenManager.refreshTokenFlow.firstOrNull() 
                ?: return Result.failure(Exception("No refresh token found"))
                
            val response = authApi.refreshToken(RefreshTokenRequest(refreshToken))
            if (response.isSuccessful) {
                response.body()?.let { refreshResponse ->
                    tokenManager.saveTokens(refreshResponse.accessToken, refreshResponse.refreshToken)
                    return Result.success(true)
                }
                Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Token refresh failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun verifyToken(): Result<Boolean> {
        return try {
            val token = tokenManager.tokenFlow.firstOrNull() 
                ?: return Result.failure(Exception("No token found"))
                
            val authHeader = "Bearer $token"
            val response = authApi.verifyToken(authHeader)
            
            if (response.isSuccessful) {
                Result.success(true)
            } else if (response.code() == 403) {
                // Token expired
                Result.failure(Exception("Token expired"))
            } else {
                Result.failure(Exception("Token verification failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun isLoggedIn(): Boolean {
        val token = tokenManager.tokenFlow.firstOrNull()
        return !token.isNullOrEmpty()
    }

    suspend fun logout() {
        tokenManager.clearTokens()
    }
}