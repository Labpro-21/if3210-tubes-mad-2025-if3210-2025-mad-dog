package com.example.purrytify.data.auth

import android.util.Base64
import android.util.Log
import org.json.JSONObject

object TokenUtils {
    private const val TAG = "TokenUtils"

    fun getTimeRemainingInSeconds(jwtToken: String?): Int? {
        if (jwtToken == null) return null

        try {
            // JWT tokens are in format: header.payload.signature
            val parts = jwtToken.split(".")
            if (parts.size != 3) return null

            // Decode the payload (middle part)
            val payloadBytes = Base64.decode(parts[1], Base64.URL_SAFE)
            val payload = String(payloadBytes)

            // Parse JSON
            val jsonObject = JSONObject(payload)

            // Get expiration time
            val expTime = jsonObject.optLong("exp", 0)
            if (expTime == 0L) return null

            // Calculate time remaining
            val currentTime = System.currentTimeMillis() / 1000
            val timeRemaining = expTime - currentTime

            return if (timeRemaining > 0) timeRemaining.toInt() else 0
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JWT token: ${e.message}")
            return null
        }
    }
}