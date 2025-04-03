package com.example.purrytify.data.model

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String
)


data class VerifyTokenResponse(
    val valid: Boolean,
    val user: UserData
)

data class UserData(
    val id: Int,
    val username: String
)