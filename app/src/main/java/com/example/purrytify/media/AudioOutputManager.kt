package com.example.purrytify.media

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Build as AndroidBuild
import android.util.Log
import com.example.purrytify.data.model.AudioOutputDevice
import com.example.purrytify.data.model.AudioOutputDeviceType

class AudioOutputManager(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    fun getAvailableAudioDevices(): List<AudioOutputDevice> {
        val rawDevices = mutableListOf<AudioOutputDevice>()
        val btAddresses = mutableSetOf<String>()
        var speakerAdded = false
        val btNameToAddress = mutableMapOf<String, String>()
        val isBluetoothA2dpOn = audioManager.isBluetoothA2dpOn
        var bluetoothDeviceName: String? = null
        if (isBluetoothA2dpOn) {
            bluetoothDeviceName = bluetoothAdapter?.bondedDevices?.firstOrNull { isBluetoothDeviceConnected(it) }?.name
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (device in audioDevices) {
                val type = when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> AudioOutputDeviceType.BLUETOOTH
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> AudioOutputDeviceType.WIRELESS_HEADSET
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> AudioOutputDeviceType.SPEAKER
                    else -> AudioOutputDeviceType.OTHER
                }
                val name = device.productName?.toString() ?: "Unknown"
                if (type == AudioOutputDeviceType.BLUETOOTH) {
                    bluetoothAdapter?.bondedDevices?.forEach { bt ->
                        if (bt.name == name) {
                            btNameToAddress[name] = bt.address
                        }
                    }
                }
                if (type == AudioOutputDeviceType.SPEAKER && !speakerAdded) {
                    if (isBluetoothA2dpOn && bluetoothDeviceName != null) {
                        rawDevices.add(
                            AudioOutputDevice(
                                id = "BT_FAKE",
                                name = "$bluetoothDeviceName (Bluetooth)",
                                type = AudioOutputDeviceType.BLUETOOTH,
                                isConnected = true
                            )
                        )
                    } else {
                        rawDevices.add(
                            AudioOutputDevice(
                                id = "SPEAKER",
                                name = "Internal Speaker",
                                type = type,
                                isConnected = device.isSink
                            )
                        )
                    }
                    speakerAdded = true
                } else if (type == AudioOutputDeviceType.BLUETOOTH) {
                    val address = btNameToAddress[name] ?: name
                    if (!btAddresses.contains(address)) {
                        rawDevices.add(
                            AudioOutputDevice(
                                id = "BT_$address",
                                name = "$name (Bluetooth)",
                                type = type,
                                isConnected = device.isSink
                            )
                        )
                        btAddresses.add(address)
                    }
                } else if (type != AudioOutputDeviceType.SPEAKER) {
                    rawDevices.add(
                        AudioOutputDevice(
                            id = "${type}_${name}_${device.id}",
                            name = "$name (${type.name})",
                            type = type,
                            isConnected = device.isSink
                        )
                    )
                }
            }
        }
        if (!speakerAdded && !isBluetoothA2dpOn) {
            rawDevices.add(
                AudioOutputDevice(
                    id = "SPEAKER",
                    name = "Internal Speaker",
                    type = AudioOutputDeviceType.SPEAKER,
                    isConnected = true
                )
            )
        }
        bluetoothAdapter?.bondedDevices?.forEach { btDevice ->
            if (isBluetoothDeviceConnected(btDevice)) {
                if (!btAddresses.contains(btDevice.address)) {
                    rawDevices.add(
                        AudioOutputDevice(
                            id = "BT_${btDevice.address}",
                            name = "${btDevice.name ?: "Bluetooth Device"} (Bluetooth)",
                            type = AudioOutputDeviceType.BLUETOOTH,
                            isConnected = true
                        )
                    )
                    btAddresses.add(btDevice.address)
                }
            }
        }
        val deduped = mutableMapOf<Pair<String, AudioOutputDeviceType>, AudioOutputDevice>()
        for (dev in rawDevices) {
            val key = dev.name to dev.type
            if (!deduped.containsKey(key)) {
                deduped[key] = dev
            } else {
                val existing = deduped[key]!!
                if (existing.type == AudioOutputDeviceType.OTHER && (dev.type == AudioOutputDeviceType.SPEAKER || dev.type == AudioOutputDeviceType.BLUETOOTH)) {
                    deduped[key] = dev
                }
            }
        }
        val result = deduped.values.toList().toMutableList()
        if (result.isEmpty()) {
            result.add(
                AudioOutputDevice(
                    id = "SPEAKER",
                    name = "Internal Speaker",
                    type = AudioOutputDeviceType.SPEAKER,
                    isConnected = true
                )
            )
        }
        Log.d("AudioOutputManager", "Returning devices: $result")
        return result
    }

    fun setAudioOutput(device: AudioOutputDevice) {
        if (device.type == AudioOutputDeviceType.BLUETOOTH) {
            audioManager.setSpeakerphoneOn(false)
            audioManager.startBluetoothSco()
            audioManager.isBluetoothScoOn = true
        } else if (device.type == AudioOutputDeviceType.SPEAKER) {
            audioManager.stopBluetoothSco()
            audioManager.isBluetoothScoOn = false
            audioManager.setSpeakerphoneOn(true)
            audioManager.mode = AudioManager.MODE_NORMAL
        } else {
            audioManager.stopBluetoothSco()
            audioManager.isBluetoothScoOn = false
            audioManager.setSpeakerphoneOn(false)
        }
    }

    fun routeToSpeaker() {
        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
        audioManager.setSpeakerphoneOn(true)
        audioManager.mode = AudioManager.MODE_NORMAL
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