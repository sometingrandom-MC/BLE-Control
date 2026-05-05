package com.samstechlab.blecontrol

import android.bluetooth.BluetoothGatt
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

class Car : BleController.BleCallback {

    private val SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
    private val AUTO_MODE_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
    private val VOLTAGE_UUID = UUID.fromString("0859e192-00ba-4690-bb66-94d2d132a255")
    private val INVERTER_STATE_UUID = UUID.fromString("83a4e976-f0ac-49b7-8212-27724af75ab6")

    val inverter = Inverter()
    val battery = Battery()
    var bleController: BleController? = null
        set(value) {
            field = value
            value?.setCallback(this)
        }

    inner class Battery {
        private var _batteryVoltage by mutableFloatStateOf(0f)

        fun getVoltage(): Float = _batteryVoltage

        internal fun updateVoltageFromBle(voltage: Float) {
            _batteryVoltage = voltage
        }
    }

    inner class Inverter {
        private var _powerState by mutableStateOf(value = false)
        private var _automaticMode by mutableStateOf(value = true)

        fun getState(): Boolean = _powerState

        fun setState(enabled: Boolean) {
            _powerState = enabled
            _automaticMode = false // Manual override turns off auto mode
            bleController?.writeCharacteristic(SERVICE_UUID, INVERTER_STATE_UUID, (if (enabled) "1" else "0").toByteArray())
            bleController?.writeCharacteristic(SERVICE_UUID, AUTO_MODE_UUID, "0".toByteArray())
        }

        fun getAutoMode(): Boolean = _automaticMode

        fun setAutoMode(enabled: Boolean) {
            _automaticMode = enabled
            bleController?.writeCharacteristic(SERVICE_UUID, AUTO_MODE_UUID, (if (enabled) "1" else "0").toByteArray())
        }

        internal fun updateStateFromBle(enabled: Boolean) {
            _powerState = enabled
        }

        internal fun updateAutoModeFromBle(enabled: Boolean) {
            _automaticMode = enabled
        }
    }

    override fun onValueUpdated(characteristicUuid: UUID, value: ByteArray) {
        when (characteristicUuid) {
            VOLTAGE_UUID -> {
                // ESP32 sends a raw float (4 bytes, little endian)
                if (value.size >= 4) {
                    val voltage = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN).float
                    battery.updateVoltageFromBle(voltage)
                }
            }
            INVERTER_STATE_UUID -> {
                // ESP32 sends a raw bool (1 byte: 1 for true, 0 for false)
                if (value.isNotEmpty()) {
                    val state = value[0].toInt() != 0
                    inverter.updateStateFromBle(state)
                }
            }
            AUTO_MODE_UUID -> {
                // ESP32 sends a raw bool (1 byte: 1 for true, 0 for false)
                if (value.isNotEmpty()) {
                    val auto = value[0].toInt() != 0
                    inverter.updateAutoModeFromBle(auto)
                }
            }
        }
    }

    override fun onConnectionStateChange(isConnected: Boolean) {
        Log.d("Car", "Connection state: $isConnected")
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt) {
        val service = gatt.getService(SERVICE_UUID)
        service?.let {
            Log.d("Car", "Service found, enabling notifications")
            bleController?.enableNotifications(it.getCharacteristic(VOLTAGE_UUID))
            bleController?.enableNotifications(it.getCharacteristic(INVERTER_STATE_UUID))
            bleController?.enableNotifications(it.getCharacteristic(AUTO_MODE_UUID))
        }
    }
}
