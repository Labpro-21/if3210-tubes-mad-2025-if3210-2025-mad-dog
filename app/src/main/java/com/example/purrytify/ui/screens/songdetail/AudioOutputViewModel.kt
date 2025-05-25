package com.example.purrytify.ui.screens.songdetail

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.os.Build
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.model.AudioOutputDevice
import com.example.purrytify.data.model.AudioOutputDeviceType
import com.example.purrytify.data.repository.AudioOutputRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AudioOutputViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AudioOutputRepository.getInstance(application)
    private val _devices = MutableStateFlow<List<AudioOutputDevice>>(emptyList())
    val devices: StateFlow<List<AudioOutputDevice>> = _devices.asStateFlow()
    private val _selectedDevice = MutableStateFlow<AudioOutputDevice?>(null)
    val selectedDevice: StateFlow<AudioOutputDevice?> = _selectedDevice.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private var bluetoothReceiverRegistered = false

    init {
        registerBluetoothReceiver(application)
    }    fun scanDevices() {
        viewModelScope.launch {
            try {
                val found = repository.getAvailableDevices()
                Log.d("AudioOutputViewModel", "Devices found: $found")
                _devices.value = found
                
                // Update selected device to current active device or first connected device
                val currentDevice = repository.getCurrentOutputDevice()
                _selectedDevice.value = currentDevice ?: found.firstOrNull { it.isConnected } ?: found.firstOrNull()
                
                Log.d("AudioOutputViewModel", "Selected device: ${_selectedDevice.value?.name}")
            } catch (e: Exception) {
                Log.e("AudioOutputViewModel", "Failed to scan devices", e)
                _error.value = "Failed to scan devices: ${e.message}"
            }
        }
    }    fun selectDevice(device: AudioOutputDevice) {
        viewModelScope.launch {
            try {
                Log.d("AudioOutputViewModel", "Selecting device: ${device.name}")
                repository.setAudioOutput(device)
                _error.value = null
                
                // Give the audio system time to switch, then refresh device state
                kotlinx.coroutines.delay(750) // Slightly longer delay for audio routing
                scanDevices()
                
                Log.d("AudioOutputViewModel", "Device selection completed for: ${device.name}")
            } catch (e: Exception) {
                Log.e("AudioOutputViewModel", "Failed to connect to device: ${device.name}", e)
                _error.value = "Failed to connect to ${device.name}: ${e.message}"
                // Still refresh to show correct state
                scanDevices()
            }
        }
    }

    fun fallbackToSpeaker() {
        viewModelScope.launch {
            try {
                repository.routeToSpeaker()
                _selectedDevice.value = null
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to fallback to speaker"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }    private fun registerBluetoothReceiver(context: Context) {
        if (bluetoothReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val action = intent?.action
                Log.d("AudioOutputViewModel", "Bluetooth event: $action")
                
                when (action) {
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        Log.d("AudioOutputViewModel", "Bluetooth device disconnected: ${device?.name}")
                        handleBluetoothDisconnection()
                    }
                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        Log.d("AudioOutputViewModel", "Bluetooth device connected: ${device?.name}")
                        // Refresh device list when new device connects
                        scanDevices()
                    }
                    BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1)
                        if (state == BluetoothAdapter.STATE_DISCONNECTED) {
                            Log.d("AudioOutputViewModel", "Bluetooth adapter disconnected")
                            handleBluetoothDisconnection()
                        }
                    }
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                        if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                            Log.d("AudioOutputViewModel", "Bluetooth turned off")
                            handleBluetoothDisconnection()
                        }
                    }
                }
            }
        }, filter)
        bluetoothReceiverRegistered = true
    }
    
    private fun handleBluetoothDisconnection() {
        val current = _selectedDevice.value
        if (current?.type == AudioOutputDeviceType.BLUETOOTH) {
            _error.value = "Bluetooth device '${current.name}' disconnected. Switched to phone speaker."
            // Force switch to speaker
            fallbackToSpeaker()
            // Refresh device list
            scanDevices()
        }
    }
} 