package com.example.purrytify

import android.app.Application
import android.util.Log
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.data.auth.TokenManager

class PurrytifyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize singletons
        TokenManager.getInstance(this)
        AuthRepository.getInstance(this)

        Log.d("PurrytifyApp", "Application initialized")
    }
}