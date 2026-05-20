package com.samstechlab.blecontrol

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
            val context = LocalContext.current
            val car = remember { Car() }
            val bleController = remember { BleController(context) }
            car.bleController = bleController

            BLEControlTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedBackground(modifier = Modifier.fillMaxSize())
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent
                    ) {
                        MainContent(car, bleController)
                    }

                    // Global Persistent Status Chip
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(bottom = 100.dp), // Height above navigation bar
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        val isConnected = bleController.isConnected
                        val isScanning = bleController.isScanning

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
                                    bleController.targetDeviceName?.let { bleController.startScan(it) } ?: bleController.startScan()
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
                            border = null,
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color.Transparent
                            ),
                            modifier = Modifier.glassEffect(cornerRadius = 25.dp, alpha = 0.2f)
                        )
                    }
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
fun MainContent(car: Car, bleController: BleController) {
    val context = LocalContext.current
    var currentScreenTitle by rememberSaveable { mutableStateOf(Screen.Main.title) }
    
    // Single state to manage picker visibility
    var showDevicePicker by rememberSaveable { 
        mutableStateOf(bleController.targetDeviceName == null) 
    }

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
                showDevicePicker = false
            },
            onDismiss = {
                showDevicePicker = false
                bleController.stopScan()
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
        },
        containerColor = Color.Transparent,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = Color.Transparent,
            navigationRailContainerColor = Color.Transparent
        )
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                (slideInVertically { height -> height / 12 } + fadeIn(animationSpec = tween(600)))
                    .togetherWith(fadeOut(animationSpec = tween(400)))
            },
            label = "screenTransition",
            modifier = Modifier.fillMaxSize()
        ) { screen ->
            val blurRadius by transition.animateDp(
                label = "blur",
                transitionSpec = { tween(600) }
            ) { state ->
                if (state == EnterExitState.Visible) 0.dp else 16.dp
            }

            Box(
                modifier = if (blurRadius > 0.dp) Modifier.blur(blurRadius).fillMaxSize() else Modifier.fillMaxSize()
            ) {
                when (screen) {
                    Screen.Main -> MainScreen(car)
                    Screen.Controls -> ControlsScreen(car)
                    Screen.Settings -> SettingsScreen(car, onChangeDevice = { showDevicePicker = true })
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DeviceSelectionDialog(
    bleController: BleController,
    onDeviceSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        bleController.startScan()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Container for the dialog content with confined background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .glassEffect(cornerRadius = 28.dp, alpha = 0.15f)
            ) {
                // Background confined strictly to the dialog area
                AnimatedBackground(modifier = Modifier.matchParentSize())

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    StaggeredEntrance(index = 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Select Device",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (bleController.isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                IconButton(onClick = { bleController.startScan() }) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Refresh",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        "Please select your BLE device to continue.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(bleController.foundDevices) { index, device ->
                            StaggeredEntrance(index = index + 1) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .glassEffect(cornerRadius = 16.dp, alpha = 0.05f)
                                        .clickable {
                                            device.name?.let { onDeviceSelected(it) }
                                        }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .glassEffect(cornerRadius = 10.dp, alpha = 0.1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Bluetooth,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = device.name ?: "Unknown Device",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = device.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                "Cancel",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    val context = LocalContext.current
    val car = remember { Car() }
    val bleController = remember { BleController(context) }
    BLEControlTheme {
        MainContent(car, bleController)
    }
}
