package com.example.purrytify.data.api

import com.example.purrytify.data.model.LoginRequest
import com.example.purrytify.data.model.LoginResponse
import com.example.purrytify.data.model.RefreshTokenRequest
import com.example.purrytify.data.model.RefreshTokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {
    @POST("api/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>
    
    @POST("api/refresh-token")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<RefreshTokenResponse>
    
    @GET("api/verify-token")
    suspend fun verifyToken(@Header("Authorization") authHeader: String): Response<Unit>
}