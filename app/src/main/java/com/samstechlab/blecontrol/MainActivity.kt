package com.samstechlab.blecontrol

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview

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
            Screen.Settings -> SettingsScreen(bleController)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BLEControlTheme {
        MainContent()
    }
}
