package com.example.purrytify

import android.app.Application
import com.example.purrytify.services.TokenServiceManager

class PurrytifyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Start the token validation service
        TokenServiceManager.startTokenValidationService(this)
    }
}