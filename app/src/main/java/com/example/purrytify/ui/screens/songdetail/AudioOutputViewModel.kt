package com.example.purrytify.ui.screens.songdetail

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.model.AudioOutputDevice
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
    }

    fun scanDevices() {
        viewModelScope.launch {
            try {
                val found = repository.getAvailableDevices()
                Log.d("AudioOutputViewModel", "Devices found: $found")
                _devices.value = found
                // Always update selectedDevice to the first connected device
                _selectedDevice.value = found.firstOrNull { it.isConnected } ?: found.firstOrNull()
            } catch (e: Exception) {
                Log.e("AudioOutputViewModel", "Failed to scan devices", e)
                _error.value = "Failed to scan devices"
            }
        }
    }

    fun selectDevice(device: AudioOutputDevice) {
        viewModelScope.launch {
            try {
                repository.setAudioOutput(device)
                _selectedDevice.value = device
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to connect to device"
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
    }

    private fun registerBluetoothReceiver(context: Context) {
        if (bluetoothReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        }
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val action = intent?.action
                if (action == BluetoothDevice.ACTION_ACL_DISCONNECTED ||
                    (action == BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED &&
                     intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1) == BluetoothAdapter.STATE_DISCONNECTED)) {
                    val current = _selectedDevice.value
                    if (current != null && current.type.name == "BLUETOOTH") {
                        _error.value = "Bluetooth device disconnected. Switched to internal speaker."
                        scanDevices()
                    }
                }
            }
        }, filter)
        bluetoothReceiverRegistered = true
    }
} 