package com.abhinavvaidya.appusagetracker.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abhinavvaidya.appusagetracker.R
import com.abhinavvaidya.appusagetracker.data.preferences.AppListMetric
import com.abhinavvaidya.appusagetracker.domain.model.AppUsageInfo
import com.abhinavvaidya.appusagetracker.ui.theme.CardBackground
import com.abhinavvaidya.appusagetracker.ui.theme.GradientPurple
import com.abhinavvaidya.appusagetracker.ui.theme.TextSecondary
import com.abhinavvaidya.appusagetracker.util.DurationFormatter

@Composable
fun AppUsageRow(
    appUsageInfo: AppUsageInfo,
    modifier: Modifier = Modifier,
    maxUsageMinutes: Long = 180L,
    listMetric: AppListMetric = AppListMetric.USAGE_TIME,
    compact: Boolean = false
) {
    val context = LocalContext.current

    val rightValue = when (listMetric) {
        AppListMetric.USAGE_TIME -> DurationFormatter.formatDetailed(context, appUsageInfo.usageTimeMillis)
        AppListMetric.LAUNCH_COUNT -> context.getString(R.string.list_metric_launch_value, appUsageInfo.launchCount)
        AppListMetric.USAGE_PERCENTAGE -> context.getString(
            R.string.list_metric_percentage_value,
            appUsageInfo.usagePercentage
        )
    }

    val rightLabel = when (listMetric) {
        AppListMetric.USAGE_TIME -> stringResource(id = R.string.list_metric_usage_time)
        AppListMetric.LAUNCH_COUNT -> stringResource(id = R.string.list_metric_launch_count)
        AppListMetric.USAGE_PERCENTAGE -> stringResource(id = R.string.list_metric_usage_percentage)
    }

    val horizontalPadding = if (compact) 10.dp else 14.dp
    val verticalPadding = if (compact) 8.dp else 12.dp
    val cardShape = if (compact) RoundedCornerShape(14.dp) else RoundedCornerShape(18.dp)
    val iconSize = if (compact) 42.dp else 52.dp

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (compact) Color.White.copy(alpha = 0.08f) else CardBackground.copy(alpha = 0.9f)
        ),
        shape = cardShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(
                    drawable = appUsageInfo.appIcon,
                    appName = appUsageInfo.appName,
                    iconSize = iconSize
                )

                Spacer(modifier = Modifier.width(if (compact) 10.dp else 12.dp))

                Column {
                    Text(
                        text = appUsageInfo.appName,
                        style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = appUsageInfo.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = rightValue,
                    style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = rightLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun AppIcon(
    drawable: Drawable?,
    appName: String,
    iconSize: Dp
) {
    val bitmap = remember(drawable) { drawable?.toBitmapSafe() }
    val cornerRadius = if (iconSize <= 42.dp) 10.dp else 12.dp

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = appName,
            modifier = Modifier
                .size(iconSize)
                .clip(RoundedCornerShape(cornerRadius))
        )
    } else {
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(RoundedCornerShape(cornerRadius))
                .background(GradientPurple.copy(alpha = 0.55f))
        )
    }
}

private fun Drawable.toBitmapSafe(): Bitmap? {
    return try {
        if (this is BitmapDrawable) {
            bitmap
        } else {
            val width = intrinsicWidth.coerceAtLeast(1)
            val height = intrinsicHeight.coerceAtLeast(1)
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { out ->
                val canvas = Canvas(out)
                setBounds(0, 0, canvas.width, canvas.height)
                draw(canvas)
            }
        }
    } catch (_: Exception) {
        null
    }
}
