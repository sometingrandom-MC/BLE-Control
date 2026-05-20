package com.samstechlab.blecontrol

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun Modifier.glassEffect(
    cornerRadius: Dp = 24.dp,
    alpha: Float = 0.05f,
    color: Color = Color.Unspecified
): Modifier {
    val finalColor = if (color == Color.Unspecified) MaterialTheme.colorScheme.surfaceVariant else color
    
    return this
        .clip(RoundedCornerShape(cornerRadius))
        .drawWithCache {
            // Pre-calculate stable noise points
            val noiseCount = (size.width * size.height / 100).toInt().coerceAtMost(1000)
            val random = Random(42)
            val noisePoints = List(noiseCount) {
                Offset(random.nextFloat() * size.width, random.nextFloat() * size.height)
            }

            val specularBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            )

            val borderBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.05f)
                ),
                start = Offset(0f, 0f),
                end = Offset(1000f, 1000f)
            )

            val cornerRadiusPx = cornerRadius.toPx()

            onDrawBehind {
                // 1. Draw stable noise texture
                noisePoints.forEach { point ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = 1f,
                        center = point
                    )
                }

                // 2. Surface specular highlight
                drawRect(brush = specularBrush)

                // 3. Base tint
                drawRect(color = finalColor.copy(alpha = alpha))

                // 4. Border (Moved into onDrawBehind to avoid extra Modifier.border pass)
                drawRoundRect(
                    brush = borderBrush,
                    cornerRadius = CornerRadius(cornerRadiusPx),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
            }
        }
}
