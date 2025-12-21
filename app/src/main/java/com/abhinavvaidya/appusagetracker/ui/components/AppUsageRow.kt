package com.abhinavvaidya.appusagetracker.ui.components

import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abhinavvaidya.appusagetracker.domain.model.AppUsageInfo
import com.abhinavvaidya.appusagetracker.ui.theme.AccentPrimary
import com.abhinavvaidya.appusagetracker.ui.theme.DarkCardElevated
import com.abhinavvaidya.appusagetracker.ui.theme.GlassBorder
import com.abhinavvaidya.appusagetracker.ui.theme.GradientCyan
import com.abhinavvaidya.appusagetracker.ui.theme.GradientPink
import com.abhinavvaidya.appusagetracker.ui.theme.GradientPurple
import com.abhinavvaidya.appusagetracker.ui.theme.TextSecondary
import com.abhinavvaidya.appusagetracker.ui.theme.UsageHigh
import com.abhinavvaidya.appusagetracker.ui.theme.UsageLow
import com.abhinavvaidya.appusagetracker.ui.theme.UsageMedium

@Composable
fun AppUsageRow(
    appUsageInfo: AppUsageInfo,
    modifier: Modifier = Modifier,
    maxUsageMinutes: Long = 180L // 3 hours as max for progress calculation
) {
    // Debug: Log icon status
    Log.d("AppUsageRow", "${appUsageInfo.appName}: icon=${appUsageInfo.appIcon != null}")

    val usageMinutes = appUsageInfo.usageTimeMillis / (1000 * 60)
    val progress = (usageMinutes.toFloat() / maxUsageMinutes).coerceIn(0f, 1f)

    // Animate progress bar
    var targetProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 800),
        label = "progress"
    )

    LaunchedEffect(progress) {
        targetProgress = progress
    }

    // Color based on usage level
    val usageColor = when {
        progress < 0.33f -> UsageLow
        progress < 0.66f -> UsageMedium
        else -> UsageHigh
    }

    val gradientColors = when {
        progress < 0.33f -> listOf(GradientCyan, UsageLow)
        progress < 0.66f -> listOf(UsageMedium, GradientPink.copy(alpha = 0.7f))
        else -> listOf(GradientPink, UsageHigh)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = AccentPrimary.copy(alpha = 0.1f),
                spotColor = AccentPrimary.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkCardElevated
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App icon with glow effect
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = GlassBorder,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    appUsageInfo.appIcon?.let { drawable ->
                        Log.d("AppUsageRow", "Rendering icon for ${appUsageInfo.appName}, type=${drawable.javaClass.simpleName}")
                        // Convert Drawable to ImageBitmap for Compose
                        val bitmap = remember(drawable) {
                            try {
                                if (drawable is BitmapDrawable) {
                                    Log.d("AppUsageRow", "  Using BitmapDrawable directly")
                                    drawable.bitmap
                                } else {
                                    // For non-bitmap drawables (e.g., VectorDrawables)
                                    Log.d("AppUsageRow", "  Converting to Bitmap, size=${drawable.intrinsicWidth}x${drawable.intrinsicHeight}")
                                    val bmp = android.graphics.Bitmap.createBitmap(
                                        drawable.intrinsicWidth.coerceAtLeast(1),
                                        drawable.intrinsicHeight.coerceAtLeast(1),
                                        android.graphics.Bitmap.Config.ARGB_8888
                                    )
                                    val canvas = android.graphics.Canvas(bmp)
                                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                                    drawable.draw(canvas)
                                    Log.d("AppUsageRow", "  Bitmap created successfully")
                                    bmp
                                }
                            } catch (e: Exception) {
                                Log.e("AppUsageRow", "Error converting drawable to bitmap for ${appUsageInfo.appName}", e)
                                null
                            }
                        }

                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = appUsageInfo.appName,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            Log.w("AppUsageRow", "Bitmap is null for ${appUsageInfo.appName}, showing placeholder")
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(GradientPurple.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            )
                        }
                    } ?: run {
                        // Placeholder when icon is null
                        Log.d("AppUsageRow", "No icon for ${appUsageInfo.appName}, showing placeholder")
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(GradientPurple.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appUsageInfo.appName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = getUsageDescription(usageMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // Time display with colored background
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(gradientColors.map { it.copy(alpha = 0.2f) }),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = appUsageInfo.formattedTime,
                        style = MaterialTheme.typography.titleMedium,
                        color = usageColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            brush = Brush.horizontalGradient(gradientColors)
                        )
                )
            }
        }
    }
}

private fun getUsageDescription(minutes: Long): String {
    return when {
        minutes < 30 -> "Light usage"
        minutes < 60 -> "Moderate usage"
        minutes < 120 -> "Heavy usage"
        else -> "Very heavy usage"
    }
}

