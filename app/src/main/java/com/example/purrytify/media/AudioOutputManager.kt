package com.example.purrytify.media

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothA2dp
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
import java.lang.reflect.Method

class AudioOutputManager(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothA2dp: BluetoothA2dp? = null
    private val handler = Handler(Looper.getMainLooper())
    private var audioFocusRequest: AudioFocusRequest? = null
    private var currentIntendedDevice: AudioOutputDeviceType = AudioOutputDeviceType.SPEAKER
    
    private var isDeviceSwitching = false
      init {
        initializeBluetoothProfile()
        
        currentIntendedDevice = when {
            audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn -> AudioOutputDeviceType.BLUETOOTH
            audioManager.isWiredHeadsetOn -> AudioOutputDeviceType.WIRELESS_HEADSET
            else -> AudioOutputDeviceType.SPEAKER
        }
        Log.d(TAG, "AudioOutputManager initialized, detected intended device: $currentIntendedDevice")
    }
    
    private fun initializeBluetoothProfile() {
        try {
            bluetoothAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    if (profile == BluetoothProfile.A2DP) {
                        bluetoothA2dp = proxy as BluetoothA2dp
                        Log.d(TAG, "Bluetooth A2DP profile connected")
                    }
                }
                
                override fun onServiceDisconnected(profile: Int) {
                    if (profile == BluetoothProfile.A2DP) {
                        bluetoothA2dp = null
                        Log.d(TAG, "Bluetooth A2DP profile disconnected")
                    }
                }
            }, BluetoothProfile.A2DP)
        } catch (e: SecurityException) {
            Log.w(TAG, "Bluetooth permission not granted, skipping profile initialization: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Bluetooth profile: ${e.message}")
        }
    }
    
    companion object {
        private const val TAG = "AudioOutputManager"
        private const val PREFERRED_DEVICE_SPEAKER = "PHONE_SPEAKER"
    }

    fun getAvailableAudioDevices(): List<AudioOutputDevice> {
        val devices = mutableListOf<AudioOutputDevice>()
        val btDevices = mutableSetOf<String>()

        val currentlyRoutedDevices = getCurrentAudioRoutingDevices()
        val speakerActive = currentlyRoutedDevices.any { 
            it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER || 
            it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE 
        } || (!audioManager.isBluetoothA2dpOn && !audioManager.isBluetoothScoOn && !audioManager.isWiredHeadsetOn)
        
        devices.add(AudioOutputDevice(
            id = PREFERRED_DEVICE_SPEAKER,
            name = "Phone Speaker",
            type = AudioOutputDeviceType.SPEAKER,
            isConnected = true // Speaker is always available
        ))

        // Add available output devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).forEach { device ->
                when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> {
                        val name = device.productName?.toString() ?: "Bluetooth Device"
                        if (!btDevices.contains(name)) {
                            val isCurrentlyRouted = currentlyRoutedDevices.any { it.id == device.id }
                            devices.add(AudioOutputDevice(
                                id = "BT_${device.id}",
                                name = name,
                                type = AudioOutputDeviceType.BLUETOOTH,
                                isConnected = isCurrentlyRouted
                            ))
                            btDevices.add(name)
                        }
                    }
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> {
                        val name = device.productName?.toString() ?: "Wired Headphones"
                        val isCurrentlyRouted = currentlyRoutedDevices.any { it.id == device.id }
                        devices.add(AudioOutputDevice(
                            id = "WIRED_${device.id}",
                            name = name,
                            type = AudioOutputDeviceType.WIRELESS_HEADSET,
                            isConnected = isCurrentlyRouted
                        ))
                    }
                }
            }
        }

        Log.d(TAG, "Available devices: ${devices.map { "${it.name} (connected=${it.isConnected})" }}")
        return devices
    }
    
    private fun getCurrentAudioRoutingDevices(): List<AudioDeviceInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).filter { device ->
                // Check if device is currently being used for audio routing
                when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> {
                        audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn
                    }
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> {
                        audioManager.isWiredHeadsetOn
                    }
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
                    AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> {
                        !audioManager.isBluetoothA2dpOn && !audioManager.isBluetoothScoOn && !audioManager.isWiredHeadsetOn
                    }
                    else -> false
                }
            }
        } else {
            emptyList()
        }
    }    fun setAudioOutput(device: AudioOutputDevice) {
        try {
            Log.d(TAG, "Setting audio output to: ${device.name} (type: ${device.type})")
            
            when (device.type) {
                AudioOutputDeviceType.BLUETOOTH -> {
                    routeToBluetoothDevice(device)
                }
                AudioOutputDeviceType.SPEAKER -> {
                    routeToInternalSpeaker()
                }
                AudioOutputDeviceType.WIRELESS_HEADSET -> {
                    routeToWiredHeadset(device)
                }
                else -> {
                    Log.w(TAG, "Unsupported device type: ${device.type}")
                    routeToInternalSpeaker() // Fallback
                }
            }
            
            Handler(Looper.getMainLooper()).postDelayed({
                Log.d(TAG, "Audio routing change completed for: ${device.name}")
            }, 500)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting audio output", e)
            routeToInternalSpeaker() // Fallback to speaker on error
        }
    }    private fun routeToBluetoothDevice(device: AudioOutputDevice) {
        currentIntendedDevice = AudioOutputDeviceType.BLUETOOTH
        Log.d(TAG, "Switching to Bluetooth mode, intended device: $currentIntendedDevice")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val bluetoothDevice = audioManager.availableCommunicationDevices
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
            
            if (bluetoothDevice != null) {
                Log.d(TAG, "Setting communication device to: ${bluetoothDevice.productName}")
                
                // First disable speaker mode
                audioManager.isSpeakerphoneOn = false
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                
                val success = audioManager.setCommunicationDevice(bluetoothDevice)
                if (!success) {
                    Log.w(TAG, "Failed to set Bluetooth communication device, using legacy method")
                    routeToBluetoothLegacy()
                }
            } else {
                Log.w(TAG, "No Bluetooth communication device available")
                routeToBluetoothLegacy()
            }
        } else {
            routeToBluetoothLegacy()
        }
    }
      private fun routeToBluetoothLegacy() {
        try {
            audioManager.isSpeakerphoneOn = false
            
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            
            @Suppress("DEPRECATION")
            audioManager.startBluetoothSco()
            @Suppress("DEPRECATION")
            audioManager.isBluetoothScoOn = true
            
            Log.d(TAG, "Started Bluetooth SCO (legacy method)")
        } catch (e: Exception) {
            Log.e(TAG, "Error with legacy Bluetooth routing", e)
        }
    }    private fun routeToInternalSpeaker() {
        currentIntendedDevice = AudioOutputDeviceType.SPEAKER
        isDeviceSwitching = true
        Log.d(TAG, "Switching to speaker mode, intended device: $currentIntendedDevice")
        
        disconnectBluetoothA2DP()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                audioManager.clearCommunicationDevice()
                Log.d(TAG, "Cleared communication device, routing to speaker")
                
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.isSpeakerphoneOn = true
                
                stopAllBluetoothConnections()
                
                Log.d(TAG, "Successfully routed to speaker (modern method)")
            } catch (e: Exception) {
                Log.w(TAG, "Failed with modern approach, using legacy method", e)
                routeToSpeakerLegacy()
            }
        } else {
            routeToSpeakerLegacy()
        }
        
        handler.postDelayed({
            verifySpeakerRouting()
            isDeviceSwitching = false
        }, 300)
    }    private fun routeToSpeakerLegacy() {
        try {
            Log.d(TAG, "Forcing speaker route with legacy method")
            
            disconnectBluetoothA2DP()
            stopAllBluetoothConnections()
            
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            
            audioManager.isSpeakerphoneOn = true
            
            Log.d(TAG, "Routed to speaker (legacy method)")
        } catch (e: Exception) {
            Log.e(TAG, "Error with legacy speaker routing", e)
        }
    }
      /**
     * Aggressively disconnects from Bluetooth A2DP profile
     */
    private fun disconnectBluetoothA2DP() {
        try {
            bluetoothA2dp?.let { a2dp ->
                val connectedDevices = try {
                    a2dp.connectedDevices
                } catch (e: SecurityException) {
                    Log.w(TAG, "No permission to access connected Bluetooth devices")
                    return
                }
                
                Log.d(TAG, "Attempting to disconnect ${connectedDevices.size} A2DP devices")
                
                connectedDevices.forEach { device ->
                    try {
                        val disconnectMethod: Method = a2dp.javaClass.getMethod("disconnect", BluetoothDevice::class.java)
                        val result = disconnectMethod.invoke(a2dp, device) as Boolean
                        Log.d(TAG, "A2DP disconnect for ${device.name}: $result")
                    } catch (e: SecurityException) {
                        Log.w(TAG, "No permission to disconnect A2DP device ${device.name}")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to disconnect A2DP device ${device.name}", e)
                        
                        // Try alternative reflection method
                        try {
                            val setActiveDeviceMethod: Method = a2dp.javaClass.getMethod("setActiveDevice", BluetoothDevice::class.java)
                            setActiveDeviceMethod.invoke(a2dp, null)
                            Log.d(TAG, "Cleared active A2DP device as fallback")
                        } catch (e2: SecurityException) {
                            Log.w(TAG, "No permission to clear active A2DP device")
                        } catch (e2: Exception) {
                            Log.w(TAG, "Failed to clear active A2DP device", e2)
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Bluetooth permission not available for A2DP disconnect")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting Bluetooth A2DP", e)
        }
    }
    
    /**
     * Stops all Bluetooth audio connections (both SCO and A2DP)
     */
    private fun stopAllBluetoothConnections() {
        try {
            // Stop SCO connection
            @Suppress("DEPRECATION")
            audioManager.stopBluetoothSco()
            @Suppress("DEPRECATION") 
            audioManager.isBluetoothScoOn = false
            
            // Try to force disable A2DP routing using reflection
            try {
                val setBluetoothA2dpOnMethod = audioManager.javaClass.getMethod("setBluetoothA2dpOn", Boolean::class.java)
                setBluetoothA2dpOnMethod.invoke(audioManager, false)
                Log.d(TAG, "Disabled A2DP routing via reflection")
            } catch (e: Exception) {
                Log.w(TAG, "Could not disable A2DP via reflection", e)
            }
            
            Log.d(TAG, "Stopped all Bluetooth connections")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Bluetooth connections", e)
        }
    }
    
    /**
     * Verifies that speaker routing is actually working and re-applies if needed
     */
    private fun verifySpeakerRouting() {
        Log.d(TAG, "Verifying speaker routing - SpeakerOn: ${audioManager.isSpeakerphoneOn}, BT_A2DP: ${audioManager.isBluetoothA2dpOn}, BT_SCO: ${audioManager.isBluetoothScoOn}")
        
        if (currentIntendedDevice == AudioOutputDeviceType.SPEAKER) {
            if (!audioManager.isSpeakerphoneOn || audioManager.isBluetoothA2dpOn) {
                Log.w(TAG, "Speaker routing verification failed, re-applying...")
                
                // More aggressive re-application
                disconnectBluetoothA2DP()
                stopAllBluetoothConnections()
                
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.isSpeakerphoneOn = true
                
                // Final verification after a delay
                handler.postDelayed({
                    Log.d(TAG, "Final verification - SpeakerOn: ${audioManager.isSpeakerphoneOn}, BT_A2DP: ${audioManager.isBluetoothA2dpOn}")
                }, 200)
            } else {
                Log.d(TAG, "Speaker routing verification successful")
            }
        }
    }
      private fun routeToWiredHeadset(device: AudioOutputDevice) {
        currentIntendedDevice = AudioOutputDeviceType.WIRELESS_HEADSET
        Log.d(TAG, "Switching to wired headset mode, intended device: $currentIntendedDevice")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Modern approach for Android 12+
            val wiredDevice = audioManager.availableCommunicationDevices
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET }
            
            if (wiredDevice != null) {
                Log.d(TAG, "Setting communication device to wired headset")
                val success = audioManager.setCommunicationDevice(wiredDevice)
                if (!success) {
                    Log.w(TAG, "Failed to set wired headset communication device")
                }
            } else {
                Log.w(TAG, "No wired headset communication device available")
            }
        } else {
            // For older versions, ensure speaker is off and let system handle wired routing
            audioManager.isSpeakerphoneOn = false
            @Suppress("DEPRECATION")
            audioManager.stopBluetoothSco()
            @Suppress("DEPRECATION")
            audioManager.isBluetoothScoOn = false
            audioManager.mode = AudioManager.MODE_NORMAL
        }
    }    fun routeToSpeaker() {
        Log.d(TAG, "Force routing to speaker")
        
        // Use the enhanced internal speaker routing
        routeToInternalSpeaker()
    }    /**
     * Gets the currently active audio output device
     */
    fun getCurrentOutputDevice(): AudioOutputDevice? {
        val currentDevices = getCurrentAudioRoutingDevices()
        
        Log.d(TAG, "getCurrentOutputDevice() - Intended: $currentIntendedDevice, Switching: $isDeviceSwitching, Mode: ${audioManager.mode}, SpeakerOn: ${audioManager.isSpeakerphoneOn}, BT_A2DP: ${audioManager.isBluetoothA2dpOn}, BT_SCO: ${audioManager.isBluetoothScoOn}")
        
        return when {
            // If we're currently switching devices, trust the intended device
            isDeviceSwitching -> {
                when (currentIntendedDevice) {
                    AudioOutputDeviceType.SPEAKER -> AudioOutputDevice(
                        id = PREFERRED_DEVICE_SPEAKER,
                        name = "Phone Speaker",
                        type = AudioOutputDeviceType.SPEAKER,
                        isConnected = true
                    )
                    AudioOutputDeviceType.BLUETOOTH -> {
                        val btDevice = currentDevices.firstOrNull { 
                            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO || it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP 
                        }
                        btDevice?.let {
                            AudioOutputDevice(
                                id = "BT_${it.id}",
                                name = it.productName?.toString() ?: "Bluetooth Device",
                                type = AudioOutputDeviceType.BLUETOOTH,
                                isConnected = true
                            )
                        }
                    }
                    else -> null
                }
            }
            // If we explicitly intended to route to speaker and speaker is on OR we're in communication mode with speaker intended
            (currentIntendedDevice == AudioOutputDeviceType.SPEAKER && audioManager.isSpeakerphoneOn) ||
            (currentIntendedDevice == AudioOutputDeviceType.SPEAKER && audioManager.mode == AudioManager.MODE_IN_COMMUNICATION) -> {
                Log.d(TAG, "Current device: Speaker (intended and active)")
                AudioOutputDevice(
                    id = PREFERRED_DEVICE_SPEAKER,
                    name = "Phone Speaker",
                    type = AudioOutputDeviceType.SPEAKER,
                    isConnected = true
                )
            }
            // If we intended Bluetooth and it's actually connected and working AND speaker is not forced on
            currentIntendedDevice == AudioOutputDeviceType.BLUETOOTH && 
            (audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn) && 
            !audioManager.isSpeakerphoneOn &&
            audioManager.mode != AudioManager.MODE_IN_COMMUNICATION -> {
                val btDevice = currentDevices.firstOrNull { 
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO || it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP 
                }
                if (btDevice != null) {
                    Log.d(TAG, "Current device: Bluetooth (intended and active)")
                    AudioOutputDevice(
                        id = "BT_${btDevice.id}",
                        name = btDevice.productName?.toString() ?: "Bluetooth Device",
                        type = AudioOutputDeviceType.BLUETOOTH,
                        isConnected = true
                    )
                } else null
            }
            // Check for wired headset (only if speaker is not forced)
            audioManager.isWiredHeadsetOn && !audioManager.isSpeakerphoneOn -> {
                val wiredDevice = currentDevices.firstOrNull { 
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES 
                }
                if (wiredDevice != null) {
                    Log.d(TAG, "Current device: Wired Headset")
                    AudioOutputDevice(
                        id = "WIRED_${wiredDevice.id}",
                        name = wiredDevice.productName?.toString() ?: "Wired Headphones",
                        type = AudioOutputDeviceType.WIRELESS_HEADSET,
                        isConnected = true
                    )
                } else null
            }
            // Default to speaker if nothing else is clearly active
            else -> {
                Log.d(TAG, "Current device: Speaker (default/fallback)")
                AudioOutputDevice(
                    id = PREFERRED_DEVICE_SPEAKER,
                    name = "Phone Speaker",
                    type = AudioOutputDeviceType.SPEAKER,
                    isConnected = true
                )
            }
        }
    }
    
    /**
     * Checks if Bluetooth audio is currently active
     */
    fun isBluetoothAudioActive(): Boolean {
        return audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn
    }
    
    /**
     * Checks if wired headset is currently connected
     */
    fun isWiredHeadsetConnected(): Boolean {
        return audioManager.isWiredHeadsetOn
    }    private fun isBluetoothDeviceConnected(device: BluetoothDevice): Boolean {
        return try {
            val method = device.javaClass.getMethod("isConnected")
            method.invoke(device) as? Boolean ?: false
        } catch (e: SecurityException) {
            Log.w(TAG, "No permission to check Bluetooth device connection status")
            false
        } catch (e: Exception) {
            Log.w(TAG, "Error checking Bluetooth device connection", e)
            false
        }
    }
    
    /**
     * Cleanup method to release Bluetooth profile proxy
     */
    fun cleanup() {
        try {
            bluetoothA2dp?.let { proxy ->
                bluetoothAdapter?.closeProfileProxy(BluetoothProfile.A2DP, proxy)
                bluetoothA2dp = null
                Log.d(TAG, "Bluetooth A2DP profile proxy closed")
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "No permission to close Bluetooth profile proxy")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing Bluetooth profile proxy", e)
        }
    }
}
