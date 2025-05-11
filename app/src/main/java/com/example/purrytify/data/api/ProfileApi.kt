package com.example.purrytify.data.api

import com.example.purrytify.data.model.ProfileResponse
import com.example.purrytify.data.model.UpdateProfileResponse

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Part

interface ProfileApi {
    @GET("/api/profile")
    suspend fun getProfile(@Header("Authorization") token: String): Response<ProfileResponse>
    
    @Multipart
    @PATCH("/api/profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Part("location") location: RequestBody,
        @Part profilePhoto: MultipartBody.Part?
    ): Response<UpdateProfileResponse>
}