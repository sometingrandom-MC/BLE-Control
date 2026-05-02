package com.samstechlab.blecontrol

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InverterTile(car: Car, modifier: Modifier = Modifier) {
    val powerState = car.inverter.getState()
    val automaticMode = car.inverter.getAutoMode()

    val statusText = if (powerState) "Running" else "Stopped"
    val modeText = if (automaticMode) " (Auto)" else ""

    val progress by animateFloatAsState(
        targetValue = if (powerState) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "progress"
    )

    QuickSettingTile(
        modifier = modifier,
        title = "Inverter",
        subtitle = statusText + modeText,
        isActive = powerState,
        onClick = {
            car.inverter.setState(!powerState)
        },
        onLongClick = {
            car.inverter.setAutoMode(!automaticMode)
        },
        icon = if (powerState) Icons.Default.Power else Icons.Default.PowerOff,
        bottomContent = {
            if (progress > 0.01f) {
                LinearWavyProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = Color.Transparent,
                    amplitude = { 1f }
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InverterSwitchCard(car: Car, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    val powerState = car.inverter.getState()
    val automaticMode = car.inverter.getAutoMode()

    val progress by animateFloatAsState(
        targetValue = if (powerState) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "progress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = { },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    car.inverter.setAutoMode(!automaticMode)
                }
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (powerState)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (powerState)
                                    Icons.Default.Power
                                else
                                    Icons.Default.PowerOff,
                                contentDescription = null,
                                tint = if (powerState)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column {
                            Text(
                                text = "Inverter Power",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val statusText = if (powerState) "Running" else "Stopped"
                            val modeText = if (automaticMode) " • Auto Mode" else ""
                            Text(
                                text = statusText + modeText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = powerState,
                        onCheckedChange = {
                            car.inverter.setState(it)
                        }
                    )
                }
            }

            if (progress > 0.01f) {
                LinearWavyProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    trackColor = Color.Transparent,
                    amplitude = { 1f }
                )
            }
        }
    }
}
