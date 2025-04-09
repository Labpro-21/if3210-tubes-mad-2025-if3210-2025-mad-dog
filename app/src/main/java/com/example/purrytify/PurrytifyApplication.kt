package com.example.purrytify

import android.app.Application
import android.util.Log
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.data.auth.TokenManager
import com.example.purrytify.services.TokenServiceManager

class PurrytifyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize singletons
        TokenManager.getInstance(this)
        AuthRepository.getInstance(this)
        TokenServiceManager.startTokenValidationService(this)

        Log.d("PurrytifyApp", "Application initialized")
    }
}