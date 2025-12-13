package com.abhinavvaidya.appusagetracker.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhinavvaidya.appusagetracker.ui.theme.AccentPrimary
import com.abhinavvaidya.appusagetracker.ui.theme.GradientCyan
import com.abhinavvaidya.appusagetracker.ui.theme.GradientPink
import com.abhinavvaidya.appusagetracker.ui.theme.GradientPurple

@Composable
fun UsageCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
    ) {
        // Animated gradient background
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            GradientPurple.copy(alpha = 0.8f),
                            AccentPrimary.copy(alpha = 0.9f),
                            GradientPink.copy(alpha = 0.7f)
                        ),
                        start = Offset(offset, 0f),
                        end = Offset(offset + 500f, 500f)
                    )
                )
        )

        // Glassmorphism overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.White.copy(alpha = 0.05f))
        )

        // Decorative blur circles
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 20.dp, top = 20.dp)
                .size(80.dp)
                .blur(40.dp)
                .background(GradientCyan.copy(alpha = 0.4f), CircleShape)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 30.dp, bottom = 10.dp)
                .size(60.dp)
                .blur(30.dp)
                .background(GradientPink.copy(alpha = 0.4f), CircleShape)
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 52.sp,
                    letterSpacing = (-2).sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtle bottom indicator
            Box(
                modifier = Modifier
                    .size(width = 60.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
    }
}

