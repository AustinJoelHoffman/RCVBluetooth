package com.example.rcvbluetooth.domain

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class BleViewModel(private val ble: BleManager) : ViewModel() {
    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices = _devices.asStateFlow()

    init {
        ble.scanResults
            .onEach { result ->
                _devices.update { currentList ->
                    if (currentList.any { it.address == result.device.address }) {
                        currentList
                    } else {
                        currentList + result.device
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    val connection = ble.connectionState
        .stateIn(viewModelScope, SharingStarted.Companion.Lazily, ConnectionState.Disconnected)

    fun startScan() {
        _devices.value = emptyList()
        ble.startScan()
    }

    fun connect(address: String) = ble.connect(address)
}