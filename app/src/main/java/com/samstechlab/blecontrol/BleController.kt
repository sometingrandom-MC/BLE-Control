package com.samstechlab.blecontrol

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.*

class BleController(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private var bluetoothGatt: BluetoothGatt? = null

    var isConnected by mutableStateOf(false)
    var isScanning by mutableStateOf(false)
    var deviceName by mutableStateOf("Not Connected")

    interface BleCallback {
        fun onValueUpdated(characteristicUuid: UUID, value: ByteArray)
        fun onConnectionStateChange(isConnected: Boolean)
        fun onServicesDiscovered(gatt: BluetoothGatt)
    }

    private var callback: BleCallback? = null

    fun setCallback(callback: BleCallback) {
        this.callback = callback
    }

    private val handler = Handler(Looper.getMainLooper())
    private var currentScanCallback: ScanCallback? = null
    
    private val descriptorWriteQueue: Queue<BluetoothGattDescriptor> = LinkedList()
    private var isWritingDescriptor = false

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.d("BleController", "Connection State Change: status=$status, newState=$newState")
            handler.post {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    isConnected = true
                    deviceName = gatt.device.name ?: "Connected"
                    bluetoothGatt = gatt
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    isConnected = false
                    deviceName = "Disconnected"
                    bluetoothGatt = null
                    descriptorWriteQueue.clear()
                    isWritingDescriptor = false
                }
                callback?.onConnectionStateChange(isConnected)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.d("BleController", "Services Discovered: status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handler.post {
                    callback?.onServicesDiscovered(gatt)
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            handler.post {
                callback?.onValueUpdated(characteristic.uuid, value)
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handler.post {
                    callback?.onValueUpdated(characteristic.uuid, value)
                }
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            Log.d("BleController", "Descriptor Write complete: status=$status")
            isWritingDescriptor = false
            handler.post { processNextDescriptor() }
        }
    }

    @SuppressLint("MissingPermission")
    private fun processNextDescriptor() {
        val gatt = bluetoothGatt ?: return
        if (isWritingDescriptor || descriptorWriteQueue.isEmpty()) return
        
        val descriptor = descriptorWriteQueue.poll() ?: return
        isWritingDescriptor = true
        val result = gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        if (result != BluetoothGatt.GATT_SUCCESS) {
            Log.e("BleController", "Failed to initiate descriptor write: $result")
            isWritingDescriptor = false
            processNextDescriptor()
        }
    }

    @SuppressLint("MissingPermission")
    fun enableNotifications(characteristic: BluetoothGattCharacteristic?) {
        val gatt = bluetoothGatt ?: return
        characteristic?.let {
            gatt.setCharacteristicNotification(it, true)
            val descriptor = it.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
            descriptor?.let { d ->
                descriptorWriteQueue.add(d)
                processNextDescriptor()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan(targetDeviceName: String) {
        if (isScanning) return
        isScanning = true
        val scanner = bluetoothAdapter.bluetoothLeScanner
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        
        currentScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (result.device.name == targetDeviceName) {
                    stopScan()
                    connect(result.device.address)
                }
            }
        }

        scanner.startScan(null, settings, currentScanCallback)
        handler.postDelayed({ stopScan() }, 10000)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!isScanning) return
        isScanning = false
        currentScanCallback?.let {
            bluetoothAdapter.bluetoothLeScanner.stopScan(it)
        }
        currentScanCallback = null
    }

    @SuppressLint("MissingPermission")
    fun connect(deviceAddress: String) {
        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
    }

    @SuppressLint("MissingPermission")
    fun writeCharacteristic(serviceUuid: UUID, characteristicUuid: UUID, data: ByteArray) {
        val service = bluetoothGatt?.getService(serviceUuid)
        val characteristic = service?.getCharacteristic(characteristicUuid)
        characteristic?.let {
            bluetoothGatt?.writeCharacteristic(it, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        }
    }
}
