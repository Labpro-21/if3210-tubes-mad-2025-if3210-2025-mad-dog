package com.example.purrytify.data.model

enum class AudioOutputDeviceType {
    BLUETOOTH, WIRELESS_HEADSET, SPEAKER, OTHER
}

data class AudioOutputDevice(
    val id: String,
    val name: String,
    val type: AudioOutputDeviceType,
    val isConnected: Boolean
) 