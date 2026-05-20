package com.samstechlab.blecontrol

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun BLEControlTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BLEControlTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent()
                }
            }
        }
    }
}

sealed class Screen(val title: String, val icon: ImageVector) {
    object Main : Screen("Main", Icons.Default.Home)
    object Controls : Screen("Controls", Icons.Default.Build)
    object Settings : Screen("Settings", Icons.Default.Settings)
}

@Composable
fun MainContent() {
    val context = LocalContext.current
    var currentScreenTitle by rememberSaveable { mutableStateOf(Screen.Main.title) }
    
    // Instantiate Car and BleController to share state between screens
    val car = remember { Car() }
    val bleController = remember { BleController(context) }
    
    // Connect them
    car.bleController = bleController
    
    val showDevicePicker = bleController.targetDeviceName == null

    // Permission handling
    val permissions = listOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    var hasPermissions by remember {
        mutableStateOf(permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.values.all { it }
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            launcher.launch(permissions.toTypedArray())
        }
    }

    if (showDevicePicker && hasPermissions) {
        DeviceSelectionDialog(
            bleController = bleController,
            onDeviceSelected = { deviceName ->
                bleController.targetDeviceName = deviceName
            }
        )
    }
    
    val items = listOf(Screen.Main, Screen.Controls, Screen.Settings)
    val currentScreen = items.find { it.title == currentScreenTitle } ?: Screen.Main

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            items.forEach { screen ->
                item(
                    icon = { Icon(screen.icon, contentDescription = screen.title) },
                    label = { Text(screen.title) },
                    selected = currentScreen == screen,
                    onClick = { currentScreenTitle = screen.title }
                )
            }
        }
    ) {
        when (currentScreen) {
            Screen.Main -> MainScreen(car)
            Screen.Controls -> ControlsScreen(car)
            Screen.Settings -> SettingsScreen(car)
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DeviceSelectionDialog(
    bleController: BleController,
    onDeviceSelected: (String) -> Unit
) {
    LaunchedEffect(Unit) {
        bleController.startScan()
    }

    AlertDialog(
        onDismissRequest = { },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Select Device")
                if (bleController.isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = { bleController.startScan() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Please select your BLE device to continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(bleController.foundDevices) { device ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    device.name?.let { onDeviceSelected(it) }
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = device.name ?: "Unknown Device",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = device.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        },
        confirmButton = { },
        dismissButton = {
            TextButton(onClick = { bleController.stopScan() }) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BLEControlTheme {
        MainContent()
    }
}
