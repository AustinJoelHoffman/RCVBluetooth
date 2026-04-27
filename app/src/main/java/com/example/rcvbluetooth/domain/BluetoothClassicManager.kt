package com.example.rcvbluetooth.domain

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
class BluetoothClassicManager(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    private var socket: BluetoothSocket? = null
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun getPairedDevices(): List<BluetoothDevice> {
        return adapter?.bondedDevices?.toList() ?: emptyList()
    }

    suspend fun connect(device: BluetoothDevice) {
        _connectionState.value = ConnectionState.Connecting
        withContext(Dispatchers.IO) {
            try {
                socket?.close()
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket?.connect()
                _connectionState.value = ConnectionState.Connected
            } catch (e: IOException) {
                e.printStackTrace()
                _connectionState.value = ConnectionState.Disconnected
                socket = null
            }
        }
    }

    fun disconnect() {
        try {
            socket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        socket = null
        _connectionState.value = ConnectionState.Disconnected
    }

    fun sendBytes(bytes: ByteArray) {
        val currentSocket = socket ?: run {
            Log.e("BT_DEBUG", "Cannot send: Socket is NULL")
            return
        }
        if (!currentSocket.isConnected) {
            Log.e("BT_DEBUG", "Cannot send: Socket is NOT CONNECTED")
            return
        }

        try {
            val hexString = bytes.joinToString(" ") { "0x%02X".format(it) }
            Log.d("BT_DEBUG", "Writing bytes: $hexString")
            val outputStream = currentSocket.outputStream
            outputStream.write(bytes)
            outputStream.flush()
            Log.d("BT_DEBUG", "Write successful")
        } catch (e: IOException) {
            Log.e("BT_DEBUG", "Write FAILED", e)
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    fun sendCommand(command: String) {
        val currentSocket = socket ?: return
        if (!currentSocket.isConnected) return

        try {
            Log.d("BT_DEBUG", "Sending command: $command")
            val outputStream = currentSocket.outputStream
            val bytes = (command + "\n").toByteArray()
            outputStream.write(bytes)
        } catch (e: IOException) {
            e.printStackTrace()
            _connectionState.value = ConnectionState.Disconnected
        }
    }
}