package com.abhinavvaidya.appusagetracker.ui.home

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.abhinavvaidya.appusagetracker.BuildConfig
import com.abhinavvaidya.appusagetracker.R
import com.abhinavvaidya.appusagetracker.domain.model.AppUsageInfo
import com.abhinavvaidya.appusagetracker.ui.components.AppUsageRow
import com.abhinavvaidya.appusagetracker.ui.theme.AccentPrimary
import com.abhinavvaidya.appusagetracker.ui.theme.GradientCyan
import com.abhinavvaidya.appusagetracker.ui.theme.GradientPink
import com.abhinavvaidya.appusagetracker.ui.theme.GradientPurple
import com.abhinavvaidya.appusagetracker.ui.theme.TextSecondary
import com.abhinavvaidya.appusagetracker.viewmodel.HomeViewModel
import kotlin.math.cos
import kotlin.math.sin

data class RingSegment(
    val app: AppUsageInfo,
    val color: Color,
    val startAngle: Float,
    val sweepAngle: Float
)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onOpenSettings: () -> Unit = {},
    onOpenWeekly: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        UsageBackground(backgroundImageUri = uiState.backgroundImageUri)

        AnimatedVisibility(
            visible = uiState.isLoading && uiState.appUsageList.isEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentPrimary)
            }
        }

        AnimatedVisibility(
            visible = !uiState.isLoading || uiState.appUsageList.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                HeaderBar(onOpenSettings = onOpenSettings)

                Spacer(modifier = Modifier.height(8.dp))

                UsageRing(
                    totalScreenTime = uiState.totalScreenTime,
                    apps = uiState.appUsageList
                )

                Spacer(modifier = Modifier.height(8.dp))

                InsightStatsRow(
                    fragmentPercent = uiState.timeFragmentPercent,
                    totalScreenTime = uiState.totalScreenTime,
                    launchCount = uiState.launchCount,
                    unlockCount = uiState.unlockCount,
                    notificationCount = uiState.notificationCount,
                    onClickDuration = onOpenWeekly
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.appUsageList.isEmpty()) {
                    EmptyListHint(modifier = Modifier.weight(1f))
                } else {
                    AppListSection(
                        apps = uiState.appUsageList,
                        listMetricOrdinal = uiState.listMetric.ordinal,
                        showPackageName = uiState.showPackageName,
                        modifier = Modifier.weight(1f)
                    )
                }

                DateNavigator(
                    dateText = uiState.selectedDateLabel,
                    canGoNewer = uiState.canGoToNewerDate,
                    canGoOlder = uiState.canGoToOlderDate,
                    onGoNewer = viewModel::showNextDate,
                    onGoOlder = viewModel::showPreviousDate
                )
            }
        }
    }
}

@Composable
private fun UsageBackground(backgroundImageUri: String?) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (!backgroundImageUri.isNullOrBlank()) {
            AsyncImage(
                model = backgroundImageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF050A21),
                                Color(0xFF04051A),
                                Color(0xFF030312)
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 170.dp, start = 36.dp)
                    .size(220.dp)
                    .clip(CircleShape)
                    .background(GradientCyan.copy(alpha = 0.17f))
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 280.dp, end = 24.dp)
                    .size(260.dp)
                    .clip(CircleShape)
                    .background(GradientPink.copy(alpha = 0.14f))
            )
        }
    }
}

@Composable
private fun HeaderBar(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.fillMaxWidth(0.5f)) {
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = stringResource(id = R.string.home_version_format, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(id = R.string.settings_header_title),
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun UsageRing(
    totalScreenTime: String,
    apps: List<AppUsageInfo>
) {
    val ringApps = apps.take(6)
    val totalMillis = ringApps.sumOf { it.usageTimeMillis }.coerceAtLeast(1L)

    val ringColors = listOf(
        Color(0xFFFF8A00),
        Color(0xFFF6E500),
        Color(0xFF1FA3FF),
        Color(0xFF1BD987),
        Color(0xFF9C66FF),
        Color(0xFFFF4FA3)
    )

    val ringSegments = remember(ringApps, totalMillis) {
        var startAngle = -90f
        ringApps.mapIndexed { index, app ->
            val sweepAngle = (app.usageTimeMillis.toFloat() / totalMillis.toFloat()) * 360f
            RingSegment(
                app = app,
                color = ringColors[index % ringColors.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle
            ).also {
                startAngle += sweepAngle
            }
        }
    }

    val ringSize = 176.dp
    val strokeWidth = 12.dp
    val iconGap = 30.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(218.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(ringSize)) {
            drawCircle(
                color = Color(0xFF6B708A),
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )

            ringSegments.forEach { segment ->
                val drawSweep = (segment.sweepAngle - 2f).coerceAtLeast(2f)
                drawArc(
                    color = segment.color,
                    startAngle = segment.startAngle,
                    sweepAngle = drawSweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        Text(
            text = totalScreenTime,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        RingIcons(
            segments = ringSegments,
            ringRadius = ringSize / 2f,
            iconGap = iconGap
        )
    }
}

@Composable
private fun RingIcons(
    segments: List<RingSegment>,
    ringRadius: Dp,
    iconGap: Dp
) {
    val density = LocalDensity.current
    val orbitRadius = ringRadius + iconGap

    segments.forEach { segment ->
        val iconBitmap = remember(segment.app.appIcon) { segment.app.appIcon?.toBitmapSafe() }
        val angle = Math.toRadians((segment.startAngle + segment.sweepAngle / 2f).toDouble())
        val x = with(density) { (cos(angle).toFloat() * orbitRadius.toPx()).toDp() }
        val y = with(density) { (sin(angle).toFloat() * orbitRadius.toPx()).toDp() }

        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap.asImageBitmap(),
                contentDescription = segment.app.appName,
                modifier = Modifier
                    .offset(x = x, y = y)
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .offset(x = x, y = y)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(GradientPurple.copy(alpha = 0.6f))
            )
        }
    }
}

@Composable
private fun InsightStatsRow(
    fragmentPercent: Int,
    totalScreenTime: String,
    launchCount: Int,
    unlockCount: Int,
    notificationCount: Int,
    onClickDuration: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatItem(
            title = stringResource(id = R.string.home_stat_fragment),
            value = "$fragmentPercent%",
            modifier = Modifier.weight(1f)
        )
        StatItem(
            title = stringResource(id = R.string.home_stat_duration),
            value = totalScreenTime,
            modifier = Modifier.weight(1f),
            clickable = true,
            onClick = onClickDuration
        )
        StatItem(
            title = stringResource(id = R.string.home_stat_launch),
            value = launchCount.toString(),
            modifier = Modifier.weight(1f)
        )
        StatItem(
            title = stringResource(id = R.string.home_stat_unlock),
            value = unlockCount.toString(),
            modifier = Modifier.weight(1f)
        )
        StatItem(
            title = stringResource(id = R.string.home_stat_notification),
            value = notificationCount.toString(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatItem(
    title: String,
    value: String,
    modifier: Modifier,
    clickable: Boolean = false,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .let {
                if (clickable) {
                    it.clickable(onClick = onClick)
                } else {
                    it
                }
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.88f),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (clickable) FontWeight.Bold else FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun AppListSection(
    apps: List<AppUsageInfo>,
    listMetricOrdinal: Int,
    showPackageName: Boolean,
    modifier: Modifier = Modifier
) {
    val listMetric = remember(listMetricOrdinal) {
        com.abhinavvaidya.appusagetracker.data.preferences.AppListMetric.entries
            .getOrElse(listMetricOrdinal) { com.abhinavvaidya.appusagetracker.data.preferences.AppListMetric.USAGE_TIME }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(id = R.string.home_app_usage_section),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.16f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentPadding = PaddingValues(bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(
                    items = apps,
                    key = { it.packageName }
                ) { app ->
                    AppUsageRow(
                        appUsageInfo = app,
                        listMetric = listMetric,
                        showPackageName = showPackageName,
                        compact = true
                    )
                }
            }
        }
    }
}

@Composable
private fun DateNavigator(
    dateText: String,
    canGoNewer: Boolean,
    canGoOlder: Boolean,
    onGoNewer: () -> Unit,
    onGoOlder: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onGoOlder, enabled = canGoOlder) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = stringResource(id = R.string.home_previous_day),
                tint = if (canGoOlder) Color.White else Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(34.dp)
            )
        }

        Text(
            text = dateText,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        IconButton(onClick = onGoNewer, enabled = canGoNewer) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = stringResource(id = R.string.home_next_day),
                tint = if (canGoNewer) Color.White else Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

@Composable
private fun EmptyListHint(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(id = R.string.home_no_usage_data),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
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
