package com.samstechlab.blecontrol

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlsScreen(car: Car) {
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Controls",
                        fontWeight = FontWeight.Bold,
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InverterSwitchCard(car = car)
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
