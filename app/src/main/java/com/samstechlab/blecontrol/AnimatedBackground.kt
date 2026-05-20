package com.samstechlab.blecontrol

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun AnimatedBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    
    val t1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "t1"
    )
    val t2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "t2"
    )
    val t3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "t3"
    )

    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    val secondaryColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
    val tertiaryColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)

    Spacer(
        modifier = modifier
            .drawWithCache {
                onDrawBehind {
                    drawRect(color = surfaceColor)

                    // Blob 1
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(primaryColor, Color.Transparent),
                            center = Offset(t1 * size.width, t2 * size.height),
                            radius = size.maxDimension * 0.8f
                        ),
                        center = Offset(t1 * size.width, t2 * size.height),
                        radius = size.maxDimension * 0.8f
                    )

                    // Blob 2
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(secondaryColor, Color.Transparent),
                            center = Offset((1 - t2) * size.width, t3 * size.height),
                            radius = size.maxDimension * 0.9f
                        ),
                        center = Offset((1 - t2) * size.width, t3 * size.height),
                        radius = size.maxDimension * 0.9f
                    )

                    // Blob 3
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(tertiaryColor, Color.Transparent),
                            center = Offset(t3 * size.width, (1 - t1) * size.height),
                            radius = size.maxDimension * 0.7f
                        ),
                        center = Offset(t3 * size.width, (1 - t1) * size.height),
                        radius = size.maxDimension * 0.7f
                    )
                }
            }
    )
}
