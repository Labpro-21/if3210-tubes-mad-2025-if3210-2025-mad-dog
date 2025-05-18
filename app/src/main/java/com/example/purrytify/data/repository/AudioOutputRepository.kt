package com.example.purrytify.data.repository

import android.content.Context
import com.example.purrytify.data.model.AudioOutputDevice
import com.example.purrytify.media.AudioOutputManager

class AudioOutputRepository private constructor(context: Context) {
    private val manager = AudioOutputManager(context)

    fun getAvailableDevices(): List<AudioOutputDevice> = manager.getAvailableAudioDevices()
    fun setAudioOutput(device: AudioOutputDevice) = manager.setAudioOutput(device)
    fun routeToSpeaker() = manager.routeToSpeaker()

    companion object {
        @Volatile
        private var INSTANCE: AudioOutputRepository? = null
        fun getInstance(context: Context): AudioOutputRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AudioOutputRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
} 