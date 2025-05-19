package com.example.purrytify.data.api

import com.example.purrytify.data.model.OnlineSongResponse

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface OnlineSongApi {
    @GET("/api/top-songs/global")
    suspend fun getTopGlobalSongs(
        @Header("Authorization") token: String
    ): Response<List<OnlineSongResponse>>

    @GET("/api/top-songs/{country_code}")
    suspend fun getTopCountrySongs(
        @Header("Authorization") token: String,
        @Path("country_code") countryCode: String
    ): Response<List<OnlineSongResponse>>

    @GET("/api/songs/{id}")
    suspend fun getSongById(
        @Header("Authorization") token: String,
        @Path("id") songId: Int
    ): Response<OnlineSongResponse>
}