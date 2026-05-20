package com.samstechlab.blecontrol

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
        .drawBehind {
            // 1. Add subtle noise/grain texture
            val noiseCount = (size.width * size.height / 100).toInt()
            val random = Random(42) // Stable seed for noise
            repeat(noiseCount) {
                val x = random.nextFloat() * size.width
                val y = random.nextFloat() * size.height
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
                    radius = 1f,
                    center = Offset(x, y)
                )
            }
            
            // 2. Subtle surface specular highlight (top-left light source)
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )
            )
            
            // 3. Base tint
            drawRect(color = finalColor.copy(alpha = alpha))
        }
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.05f)
                ),
                start = Offset(0f, 0f),
                end = Offset(1000f, 1000f)
            ),
            shape = RoundedCornerShape(cornerRadius)
        )
}
