package com.example.purrytify.media

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.purrytify.data.model.AudioOutputDevice
import com.example.purrytify.data.model.AudioOutputDeviceType

class AudioOutputManager(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    fun getAvailableAudioDevices(): List<AudioOutputDevice> {
        val devices = mutableListOf<AudioOutputDevice>()
        val btDevices = mutableSetOf<String>()

        // Always add phone speaker, but mark it as connected only if Bluetooth is inactive
        val isBluetoothActive = audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn
        devices.add(AudioOutputDevice(
            id = "PHONE_SPEAKER",
            name = "Phone Speaker",
            type = AudioOutputDeviceType.SPEAKER,
            isConnected = !isBluetoothActive // Speaker is connected if Bluetooth is off
        ))

        // Add Bluetooth devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).forEach { device ->
                if (device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO || 
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) {
                    val name = device.productName?.toString() ?: "Bluetooth"
                    if (!btDevices.contains(name)) {
                        devices.add(AudioOutputDevice(
                            id = "BT_${device.id}",
                            name = name,
                            type = AudioOutputDeviceType.BLUETOOTH,
                            isConnected = device.isSink
                        ))
                        btDevices.add(name)
                    }
                }
            }
        }

        return devices
    }

    fun setAudioOutput(device: AudioOutputDevice) {
        when (device.type) {
            AudioOutputDeviceType.BLUETOOTH -> {
                // 1. Disable speaker first
                audioManager.isSpeakerphoneOn = false
                audioManager.setSpeakerphoneOn(false)

                // 2. Enable Bluetooth
                audioManager.mode = AudioManager.MODE_NORMAL
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    audioManager.clearCommunicationDevice()
                    audioManager.availableCommunicationDevices
                        .firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
                        ?.let { audioManager.setCommunicationDevice(it) }
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.startBluetoothSco()
                    @Suppress("DEPRECATION")
                    audioManager.isBluetoothScoOn = true
                }
                audioManager.setParameters("audio_output=bluetooth")
            }
            else -> { // For SPEAKER
                // 1. Disable Bluetooth completely
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    audioManager.clearCommunicationDevice()
                }
                @Suppress("DEPRECATION")
                audioManager.stopBluetoothSco()
                @Suppress("DEPRECATION")
                audioManager.isBluetoothScoOn = false
                audioManager.setParameters("bluetooth_sco=off")
                audioManager.setParameters("bluetooth_enabled=false")

                // 2. Force speaker mode
                audioManager.mode = AudioManager.MODE_NORMAL
                audioManager.isSpeakerphoneOn = true
                audioManager.setSpeakerphoneOn(true)
                audioManager.setParameters("audio_output=speaker")

                // 3. Disable Bluetooth at the adapter level (requires BLUETOOTH_ADMIN permission)
                try {
                    bluetoothAdapter?.disable()
                } catch (e: SecurityException) {
                    Log.e("AudioOutputManager", "BLUETOOTH_ADMIN permission missing", e)
                }
            }
        }
    }

    fun routeToSpeaker() {
        audioManager.mode = AudioManager.MODE_NORMAL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.availableCommunicationDevices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            }?.let { speaker ->
                audioManager.setCommunicationDevice(speaker)
            } ?: run {
                audioManager.isSpeakerphoneOn = true
            }
        } else {
            audioManager.stopBluetoothSco()
            audioManager.isBluetoothScoOn = false
            audioManager.isSpeakerphoneOn = true
        }
    }

    private fun isBluetoothDeviceConnected(device: BluetoothDevice): Boolean {
        return try {
            val method = device.javaClass.getMethod("isConnected")
            method.invoke(device) as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }
}
