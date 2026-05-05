package com.samstechlab.blecontrol

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(bleController: BleController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "BLE Status: ${if (bleController.isConnected) "Connected" else "Disconnected"}", style = MaterialTheme.typography.titleLarge)
        Text(text = "Device: ${bleController.deviceName}", style = MaterialTheme.typography.bodyMedium)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (bleController.isScanning) {
            Text(text = "Scanning...")
            Button(onClick = { bleController.stopScan() }) {
                Text("Stop Scan")
            }
        } else {
            Button(onClick = { bleController.startScan("Mercedes-Benz S430") }) {
                Text("Start Scan")
            }
        }
        
        if (bleController.isConnected) {
            Button(onClick = { bleController.disconnect() }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Disconnect")
            }
        }
    }
}
