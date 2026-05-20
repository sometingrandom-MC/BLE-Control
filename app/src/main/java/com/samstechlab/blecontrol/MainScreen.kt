package com.samstechlab.blecontrol

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(car: Car) {
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "BLE Control",
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Quick Settings",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InverterTile(car = car, modifier = Modifier.weight(1f))
                
                // Battery Voltage Tile
                QuickSettingTile(
                    modifier = Modifier.weight(1f),
                    title = "Battery",
                    subtitle = "%.1f V".format(car.battery.getVoltage()),
                    isActive = false, // Always off/neutral
                    onClick = { },
                    icon = Icons.Default.BatteryFull
                )
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            val ble = car.bleController
            val isConnected = ble?.isConnected ?: false
            val isScanning = ble?.isScanning ?: false

            val statusText = when {
                isConnected -> "Connected"
                isScanning -> "Scanning..."
                else -> "Disconnected"
            }

            val statusColor = when {
                isConnected -> Color(0xFF4CAF50) // Green
                isScanning -> Color(0xFFFFC107) // Amber
                else -> MaterialTheme.colorScheme.error
            }

            AssistChip(
                label = { Text(statusText) },
                onClick = {
                    if (!isConnected && !isScanning) {
                        ble?.targetDeviceName?.let { ble.startScan(it) } ?: ble?.startScan()
                    }
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Circle,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(10.dp)
                    )
                },
                shape = RoundedCornerShape(25.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            )
        }
    }
}
