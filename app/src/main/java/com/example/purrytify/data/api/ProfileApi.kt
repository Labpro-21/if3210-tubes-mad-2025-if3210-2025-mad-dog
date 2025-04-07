package com.example.purrytify.data.api

import com.example.purrytify.data.model.ProfileResponse

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface ProfileApi {
    @GET("/api/profile")
    suspend fun getProfile(@Header("Authorization") token: String): Response<ProfileResponse>
}