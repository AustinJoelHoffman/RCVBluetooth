package com.example.rcvbluetooth.domain

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class OpCode(val value: Byte) {
    STOP(0x00),
    FORWARD(0x01),
    BACKWARD(0x02),
    LEFT(0x03),
    RIGHT(0x04),
    SPIN_LEFT(0x05),
    SPIN_RIGHT(0x06),
    SPEED(0x07),
    HEARTBEAT(0x08)
}

class ControlViewModel(private val bluetoothManager: BluetoothClassicManager) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = bluetoothManager.connectionState
    
    private var heartbeatJob: Job? = null

    init {
        // Observe heartbeat
        viewModelScope.launch {
            connectionState.collect { state ->
                if (state == ConnectionState.Connected) {
                    startHeartbeat()
                } else {
                    stopHeartbeat()
                }
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            while (isActive) {
                delay(500)
                bluetoothManager.sendBytes(byteArrayOf(OpCode.HEARTBEAT.value))
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    fun getPairedDevices(): List<BluetoothDevice> {
        return bluetoothManager.getPairedDevices()
    }

    fun connect(device: BluetoothDevice) {
        viewModelScope.launch {
            bluetoothManager.connect(device)
        }
    }

    fun disconnect() {
        bluetoothManager.disconnect()
    }

    fun sendCommand(opCode: OpCode) {
        bluetoothManager.sendBytes(byteArrayOf(opCode.value))
    }

    fun sendSpeed(speed: Int) {
        bluetoothManager.sendBytes(byteArrayOf(OpCode.SPEED.value, speed.toByte()))
    }

    override fun onCleared() {
        super.onCleared()
        stopHeartbeat()
    }
}

class ControlViewModelFactory(private val bluetoothManager: BluetoothClassicManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ControlViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ControlViewModel(bluetoothManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
