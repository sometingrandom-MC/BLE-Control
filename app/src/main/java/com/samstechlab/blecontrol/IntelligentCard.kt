package com.samstechlab.blecontrol

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

data class Insight(
    val text: String,
    val icon: ImageVector,
    val color: Color? = null,
    val priority: Int = 2, // 0: Critical, 1: Warning, 2: Info
    val stableKey: String
)

@Composable
fun TypewriterText(
    text: String,
    stableKey: Any,
    modifier: Modifier = Modifier,
    delayMillis: Long = 40,
    onAnimationFinish: () -> Unit = {},
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
    lineHeight: androidx.compose.ui.unit.TextUnit = style.lineHeight
) {
    var visibleCharCount by remember(stableKey) { mutableIntStateOf(0) }
    
    LaunchedEffect(stableKey) {
        visibleCharCount = 0
        for (i in text.indices) {
            delay(delayMillis)
            visibleCharCount = i + 1
        }
        onAnimationFinish()
    }

    // If stableKey is the same, we show the text up to visibleCharCount.
    // If typing is finished (or if the new text is shorter than what we've "typed"),
    // we just show the text as is (or capped).
    val displayedText = if (visibleCharCount >= text.length) {
        text
    } else {
        text.take(visibleCharCount)
    }

    Text(
        text = displayedText,
        modifier = modifier,
        style = style,
        color = color,
        lineHeight = lineHeight
    )
}

@Composable
fun IntelligentCard(car: Car, modifier: Modifier = Modifier) {
    val targetInsight = rememberMostImportantInsight(car) ?: return
    
    var displayedInsight by remember { mutableStateOf(targetInsight) }
    var isTyping by remember { mutableStateOf(true) }
    var isInBufferPeriod by remember { mutableStateOf(false) }

    // Logic to transition between insights with a buffer
    LaunchedEffect(targetInsight) {
        // If we are already showing this stable category, just update the data immediately
        if (targetInsight.stableKey == displayedInsight.stableKey) {
            displayedInsight = targetInsight
            return@LaunchedEffect
        }

        // If it's a new category, we must wait for current one to finish typing + buffer
        while (isTyping || isInBufferPeriod) {
            delay(100)
        }
        
        displayedInsight = targetInsight
        isTyping = true
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassEffect(cornerRadius = 24.dp, alpha = 0.15f)
            .aiGlow(isAutoMode = true, cornerRadius = 24.dp)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "SYSTEM INSIGHTS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        AnimatedContent(
            targetState = displayedInsight.stableKey,
            transitionSpec = {
                fadeIn(animationSpec = androidx.compose.animation.core.tween(500)) togetherWith 
                fadeOut(animationSpec = androidx.compose.animation.core.tween(500))
            },
            label = "insightAnimation"
        ) { _ ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = displayedInsight.icon,
                    contentDescription = null,
                    tint = displayedInsight.color ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
                
                TypewriterText(
                    text = displayedInsight.text,
                    stableKey = displayedInsight.stableKey,
                    onAnimationFinish = {
                        isTyping = false
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2f
                )
            }
        }
    }

    // Buffer logic
    LaunchedEffect(isTyping) {
        if (!isTyping) {
            isInBufferPeriod = true
            delay(2000)
            isInBufferPeriod = false
        }
    }
}

@Composable
fun rememberMostImportantInsight(car: Car): Insight? {
    val insights = mutableListOf<Insight>()

    // Battery Insights
    val voltage = car.battery.getVoltage()
    when {
        voltage == 0f -> { }
        voltage < 11.8f -> {
            insights.add(
                Insight(
                    text = "Battery voltage is low (%.1fV). Consider starting the engine to recharge.".format(voltage),
                    icon = Icons.Default.BatteryAlert,
                    color = MaterialTheme.colorScheme.error,
                    priority = 0,
                    stableKey = "battery_low"
                )
            )
        }
        voltage < 12.4f -> {
            insights.add(
                Insight(
                    text = "Battery is at %.1fV. It's stable but keep an eye on usage.".format(voltage),
                    icon = Icons.Default.Bolt,
                    color = Color(0xFFFBC02D), // Amber
                    priority = 1,
                    stableKey = "battery_warning"
                )
            )
        }
        else -> {
            insights.add(
                Insight(
                    text = "Your 12V battery is healthy and fully charged at %.1fV.".format(voltage),
                    icon = Icons.Default.BatteryChargingFull,
                    color = Color(0xFF4CAF50), // Green
                    priority = 2,
                    stableKey = "battery_healthy"
                )
            )
        }
    }

    // Inverter Insights
    val powerState = car.inverter.getState()
    if (powerState) {
        insights.add(
            Insight(
                text = "The inverter is currently providing power to your devices.",
                icon = Icons.Default.Power,
                color = MaterialTheme.colorScheme.primary,
                priority = 2,
                stableKey = "inverter_on"
            )
        )
    } else {
        insights.add(
            Insight(
                text = "The inverter is powered down to save energy.",
                icon = Icons.Default.PowerOff,
                priority = 2,
                stableKey = "inverter_off"
            )
        )
    }

    return insights.minByOrNull { it.priority }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun IntelligentCardPreview() {
    val car = remember { Car() }
    MaterialTheme {
        IntelligentCard(car = car, modifier = Modifier.padding(16.dp))
    }
}
