package com.example.purrytify.data.model

data class ProfileResponse(
    val id: String,
    val username: String,
    val email: String,
    var profilePhoto: String,
    val location: String,
    val createdAt: String,
    val updatedAt: String,
)
