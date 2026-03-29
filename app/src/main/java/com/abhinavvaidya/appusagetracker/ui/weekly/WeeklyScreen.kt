package com.abhinavvaidya.appusagetracker.ui.weekly

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.abhinavvaidya.appusagetracker.R
import com.abhinavvaidya.appusagetracker.domain.model.AppUsageInfo
import com.abhinavvaidya.appusagetracker.ui.theme.AccentPrimary
import com.abhinavvaidya.appusagetracker.ui.theme.GradientCyan
import com.abhinavvaidya.appusagetracker.ui.theme.GradientPink
import com.abhinavvaidya.appusagetracker.ui.theme.TextSecondary
import com.abhinavvaidya.appusagetracker.util.DurationFormatter
import com.abhinavvaidya.appusagetracker.viewmodel.WeeklyDayUsage
import com.abhinavvaidya.appusagetracker.viewmodel.WeeklyViewModel

@Composable
fun WeeklyScreen(
    viewModel: WeeklyViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        WeeklyBackground(uiState.backgroundImageUri)

        AnimatedVisibility(
            visible = uiState.isLoading && uiState.dayUsages.isEmpty(),
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
            visible = uiState.dayUsages.isNotEmpty() || !uiState.isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                WeeklyHeader(
                    onNavigateBack = onNavigateBack
                )

                Spacer(modifier = Modifier.height(10.dp))

                SevenDayUsageChart(
                    dayUsages = uiState.dayUsages,
                    totalWeeklyTime = uiState.totalWeeklyTime,
                    averageDailyTimeMillis = uiState.averageDailyTimeMillis
                )

                Spacer(modifier = Modifier.height(10.dp))

                WeeklyTopAppList(
                    apps = uiState.topApps,
                    showPackageName = uiState.showPackageName,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                DateRangeFooter(
                    startDate = uiState.rangeStartDate,
                    endDate = uiState.rangeEndDate
                )
            }
        }
    }
}

@Composable
private fun WeeklyBackground(backgroundImageUri: String?) {
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
                    .background(Color(0x55000000))
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
                    .padding(top = 90.dp, start = 18.dp)
                    .size(170.dp)
                    .clip(CircleShape)
                    .background(GradientCyan.copy(alpha = 0.13f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 260.dp, end = 16.dp)
                    .size(220.dp)
                    .clip(CircleShape)
                    .background(GradientPink.copy(alpha = 0.12f))
            )
        }
    }
}

@Composable
private fun WeeklyHeader(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(id = R.string.about_back),
                tint = Color.White
            )
        }

        Text(
            text = stringResource(id = R.string.weekly_7day_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun SevenDayUsageChart(
    dayUsages: List<WeeklyDayUsage>,
    totalWeeklyTime: String,
    averageDailyTimeMillis: Long
) {
    val maxMillis = remember(dayUsages, averageDailyTimeMillis) {
        maxOf(
            dayUsages.maxOfOrNull { it.totalTimeMillis } ?: 1L,
            averageDailyTimeMillis.coerceAtLeast(1L)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.88f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = stringResource(id = R.string.weekly_total_legend, totalWeeklyTime),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF475569),
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                val topPadding = 8.dp.toPx()
                val bottomPadding = 18.dp.toPx()
                val chartHeight = size.height - topPadding - bottomPadding
                val barCount = dayUsages.size.coerceAtLeast(1)
                val spacing = 8.dp.toPx()
                val barWidth = ((size.width - spacing * (barCount + 1)) / barCount).coerceAtLeast(8.dp.toPx())

                val avgRatio = averageDailyTimeMillis.toFloat() / maxMillis.toFloat()
                val avgY = topPadding + chartHeight * (1f - avgRatio.coerceIn(0f, 1f))

                drawLine(
                    color = Color(0xFF94A3B8),
                    start = androidx.compose.ui.geometry.Offset(0f, avgY),
                    end = androidx.compose.ui.geometry.Offset(size.width, avgY),
                    strokeWidth = 1.dp.toPx()
                )

                dayUsages.forEachIndexed { index, dayUsage ->
                    val left = spacing + index * (barWidth + spacing)
                    val ratio = dayUsage.totalTimeMillis.toFloat() / maxMillis.toFloat()
                    val barHeight = chartHeight * ratio.coerceIn(0f, 1f)
                    val top = topPadding + chartHeight - barHeight

                    drawRoundRect(
                        color = Color(0xFF0EA5E9),
                        topLeft = androidx.compose.ui.geometry.Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight.coerceAtLeast(2.dp.toPx())),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }

                drawRect(
                    color = Color(0xFFE2E8F0),
                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - bottomPadding),
                    size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                dayUsages.forEach { dayUsage ->
                    Text(
                        text = dayUsage.displayDate,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyTopAppList(
    apps: List<AppUsageInfo>,
    showPackageName: Boolean,
    modifier: Modifier = Modifier
) {
    val maxUsageMillis = remember(apps) { apps.maxOfOrNull { it.usageTimeMillis }?.coerceAtLeast(1L) ?: 1L }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.16f))
    ) {
        if (apps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.home_no_usage_data),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(
                    items = apps,
                    key = { it.packageName }
                ) { app ->
                    WeeklyAppRow(
                        app = app,
                        maxUsageMillis = maxUsageMillis,
                        showPackageName = showPackageName
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyAppRow(
    app: AppUsageInfo,
    maxUsageMillis: Long,
    showPackageName: Boolean
) {
    val progress = (app.usageTimeMillis.toFloat() / maxUsageMillis.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val bitmap = remember(app.appIcon) { app.appIcon?.toBitmapSafe() }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.4f))
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1
                )
                if (showPackageName) {
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = DurationFormatter.formatCompact(androidx.compose.ui.platform.LocalContext.current, app.usageTimeMillis),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.End
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.24f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(4.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF1FC4FF))
            )
        }
    }
}

@Composable
private fun DateRangeFooter(
    startDate: String,
    endDate: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = startDate,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = stringResource(id = R.string.weekly_usage_duration_label),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = endDate,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Medium
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
