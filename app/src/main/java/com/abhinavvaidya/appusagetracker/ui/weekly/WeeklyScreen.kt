package com.abhinavvaidya.appusagetracker.ui.weekly

import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhinavvaidya.appusagetracker.domain.model.DailyUsageSummary
import com.abhinavvaidya.appusagetracker.ui.components.AppUsageRow
import com.abhinavvaidya.appusagetracker.ui.components.SectionHeader
import com.abhinavvaidya.appusagetracker.ui.components.UsageCard
import com.abhinavvaidya.appusagetracker.ui.theme.AccentPrimary
import com.abhinavvaidya.appusagetracker.ui.theme.AccentSecondary
import com.abhinavvaidya.appusagetracker.ui.theme.DarkCardElevated
import com.abhinavvaidya.appusagetracker.ui.theme.GlassBorder
import com.abhinavvaidya.appusagetracker.ui.theme.GradientBlue
import com.abhinavvaidya.appusagetracker.ui.theme.GradientCyan
import com.abhinavvaidya.appusagetracker.ui.theme.GradientPink
import com.abhinavvaidya.appusagetracker.ui.theme.TextSecondary
import com.abhinavvaidya.appusagetracker.viewmodel.WeeklyViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun WeeklyScreen(
    viewModel: WeeklyViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val packageManager = remember { context.packageManager }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Auto-refresh on resume
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

    val topAppsWithIcons = remember(uiState.topApps) {
        uiState.topApps.map { app ->
            app.copy(
                appIcon = try {
                    packageManager.getApplicationIcon(app.packageName)
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            )
        }
    }

    val maxUsageMinutes = remember(topAppsWithIcons) {
        topAppsWithIcons.maxOfOrNull { it.usageTimeMillis / (1000 * 60) } ?: 180L
    }

    // Calculate max daily usage for progress bars
    val maxDailyMinutes = remember(uiState.dailySummaries) {
        uiState.dailySummaries.maxOfOrNull { it.totalTimeMillis / (1000 * 60) } ?: 240L
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background gradient decoration
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 150.dp, start = 20.dp)
                .size(180.dp)
                .blur(90.dp)
                .background(GradientCyan.copy(alpha = 0.12f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 250.dp, end = 30.dp)
                .size(140.dp)
                .blur(70.dp)
                .background(GradientPink.copy(alpha = 0.1f), CircleShape)
        )

        AnimatedVisibility(
            visible = uiState.isLoading && uiState.dailySummaries.isEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = AccentSecondary,
                    strokeWidth = 3.dp
                )
            }
        }

        AnimatedVisibility(
            visible = uiState.dailySummaries.isNotEmpty() || !uiState.isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    WeeklyHeaderSection()
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    UsageCard(
                        title = "Weekly Screen Time",
                        value = uiState.totalWeeklyTime
                    )
                }

                item {
                    SectionHeader(title = "Daily Breakdown")
                }

                itemsIndexed(
                    items = uiState.dailySummaries,
                    key = { _, item -> item.date }
                ) { index, summary ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(300, delayMillis = index * 50)) +
                                slideInVertically(tween(300, delayMillis = index * 50)) { it / 2 }
                    ) {
                        DailyUsageCard(
                            summary = summary,
                            maxMinutes = maxDailyMinutes
                        )
                    }
                }

                if (topAppsWithIcons.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Top Apps This Week")
                    }

                    itemsIndexed(
                        items = topAppsWithIcons,
                        key = { _, item -> item.packageName }
                    ) { index, app ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(300, delayMillis = index * 50)) +
                                    slideInVertically(tween(300, delayMillis = index * 50)) { it / 2 }
                        ) {
                            AppUsageRow(
                                appUsageInfo = app,
                                maxUsageMinutes = maxUsageMinutes.coerceAtLeast(60L)
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
private fun WeeklyHeaderSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(GradientBlue, GradientCyan)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.size(14.dp))
            Column {
                Text(
                    text = "Weekly Report",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Last 7 days overview",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun DailyUsageCard(
    summary: DailyUsageSummary,
    maxMinutes: Long
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

    val displayDate = try {
        val date = dateFormat.parse(summary.date)
        date?.let { displayFormat.format(it) } ?: summary.date
    } catch (e: Exception) {
        summary.date
    }

    val usageMinutes = summary.totalTimeMillis / (1000 * 60)
    val progress = (usageMinutes.toFloat() / maxMinutes).coerceIn(0f, 1f)

    // Animate progress bar
    var targetProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 800),
        label = "dailyProgress"
    )

    LaunchedEffect(progress) {
        targetProgress = progress
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = AccentSecondary.copy(alpha = 0.08f),
                spotColor = AccentSecondary.copy(alpha = 0.08f)
            ),
        shape = RoundedCornerShape(18.dp),
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Day indicator
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = GlassBorder,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayDate.take(3),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = AccentSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = displayDate,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    GradientBlue.copy(alpha = 0.2f),
                                    GradientCyan.copy(alpha = 0.2f)
                                )
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = summary.formattedTotalTime,
                        style = MaterialTheme.typography.titleMedium,
                        color = AccentSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(Color.White.copy(alpha = 0.06f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(5.dp)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(GradientBlue, GradientCyan)
                            )
                        )
                )
            }
        }
    }
}

