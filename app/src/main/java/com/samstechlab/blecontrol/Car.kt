package com.samstechlab.blecontrol

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class Car {
    val inverter = Inverter()

    class Inverter {
        private var _powerState by mutableStateOf(value = true)
        private var _automaticMode by mutableStateOf(value = false)

        fun getState(): Boolean = _powerState

        fun setState(enabled: Boolean) {
            _powerState = enabled
            _automaticMode = false // Manual override turns off auto mode
        }

        fun getAutoMode(): Boolean = _automaticMode

        fun setAutoMode(enabled: Boolean) {
            _automaticMode = enabled
        }
    }
}
