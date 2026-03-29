package com.abhinavvaidya.appusagetracker.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.abhinavvaidya.appusagetracker.R
import com.abhinavvaidya.appusagetracker.data.preferences.AppListMetric
import com.abhinavvaidya.appusagetracker.data.preferences.WidgetTheme
import com.abhinavvaidya.appusagetracker.ui.theme.AccentPrimary
import com.abhinavvaidya.appusagetracker.ui.theme.CardBackground
import com.abhinavvaidya.appusagetracker.ui.theme.GradientCyan
import com.abhinavvaidya.appusagetracker.ui.theme.TextSecondary
import com.abhinavvaidya.appusagetracker.viewmodel.WidgetSettingsViewModel
import java.util.Locale

private enum class AppLanguage(
    val tag: String,
    @StringRes val labelRes: Int
) {
    Chinese("zh", R.string.language_chinese),
    English("en", R.string.language_english)
}

private fun WidgetTheme.shortNameRes(): Int = when (this) {
    WidgetTheme.DARK_MINIMAL -> R.string.theme_dark_short
    WidgetTheme.NEON_CYBER -> R.string.theme_neon_short
    WidgetTheme.SOFT_PASTEL -> R.string.theme_pastel_short
}

private fun getCurrentLanguage(): AppLanguage {
    val localeTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    return when {
        localeTags.startsWith("en") -> AppLanguage.English
        localeTags.startsWith("zh") -> AppLanguage.Chinese
        Locale.getDefault().language.startsWith("en") -> AppLanguage.English
        else -> AppLanguage.Chinese
    }
}

@Composable
fun WidgetSettingsScreen(
    viewModel: WidgetSettingsViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentLanguage = getCurrentLanguage()
    val context = LocalContext.current

    var excludedPackagesInput by rememberSaveable { mutableStateOf("") }
    var packageValidationError by rememberSaveable { mutableStateOf<String?>(null) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Ignore and keep temporary permission fallback.
            }
            viewModel.setBackgroundImageUri(uri.toString())
        }
    }

    LaunchedEffect(uiState.excludedPackages) {
        excludedPackagesInput = uiState.excludedPackages.joinToString("\n")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        SettingsHeader(onNavigateBack = onNavigateBack)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = R.string.settings_widget_theme_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WidgetTheme.entries.forEach { theme ->
                ThemeCard(
                    theme = theme,
                    isSelected = uiState.selectedTheme == theme,
                    onClick = { viewModel.setTheme(theme) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = R.string.settings_language_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppLanguage.entries.forEach { language ->
                LanguageOption(
                    text = stringResource(id = language.labelRes),
                    selected = currentLanguage == language,
                    onClick = {
                        if (currentLanguage != language) {
                            AppCompatDelegate.setApplicationLocales(
                                LocaleListCompat.forLanguageTags(language.tag)
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = R.string.settings_background_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.settings_background_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        BackgroundImageCard(
            backgroundImageUri = uiState.backgroundImageUri,
            onPickImage = { pickImageLauncher.launch(arrayOf("image/*")) },
            onClearImage = { viewModel.setBackgroundImageUri(null) }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(id = R.string.settings_daily_goal_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.settings_daily_goal_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        GoalSlider(
            currentGoalHours = uiState.dailyGoalHours,
            onGoalChange = { viewModel.setDailyGoal(it) }
        )

        Spacer(modifier = Modifier.height(32.dp))

        SettingsToggle(
            title = stringResource(id = R.string.settings_show_top_app_title),
            description = stringResource(id = R.string.settings_show_top_app_desc),
            isEnabled = uiState.showTopApp,
            onToggle = { viewModel.setShowTopApp(it) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = R.string.settings_list_metric_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.settings_list_metric_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        MetricSelector(
            selectedMetric = uiState.appListMetric,
            onMetricSelected = { viewModel.setAppListMetric(it) }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(id = R.string.settings_filter_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.settings_filter_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsToggle(
            title = stringResource(id = R.string.settings_exclude_system_title),
            description = stringResource(id = R.string.settings_exclude_system_desc),
            isEnabled = uiState.excludeSystemApps,
            onToggle = { viewModel.setExcludeSystemApps(it) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        ExcludedPackagesEditor(
            inputValue = excludedPackagesInput,
            savedPackagesCount = uiState.excludedPackages.size,
            validationError = packageValidationError,
            onInputChange = {
                excludedPackagesInput = it
                packageValidationError = null
            },
            onSave = {
                val parseResult = WidgetSettingsViewModel.parsePackageInput(excludedPackagesInput)
                if (parseResult.invalidPackages.isNotEmpty()) {
                    val sample = parseResult.invalidPackages.take(3).joinToString(", ")
                    packageValidationError = context.getString(
                        R.string.settings_package_validation_error,
                        sample
                    )
                } else {
                    viewModel.setExcludedPackages(parseResult.validPackages)
                    packageValidationError = null
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        AboutOption(onClick = onNavigateToAbout)

        Spacer(modifier = Modifier.height(16.dp))

        InfoCard()

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun SettingsHeader(onNavigateBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 12.dp)
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(id = R.string.about_back),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Box(
            modifier = Modifier
                .size(46.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(AccentPrimary, GradientCyan)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = stringResource(id = R.string.settings_header_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(id = R.string.settings_header_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun BackgroundImageCard(
    backgroundImageUri: String?,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (!backgroundImageUri.isNullOrBlank()) {
                    AsyncImage(
                        model = backgroundImageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = stringResource(id = R.string.settings_background_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (backgroundImageUri.isNullOrBlank()) {
                    stringResource(id = R.string.settings_background_not_set)
                } else {
                    stringResource(id = R.string.settings_background_set)
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onPickImage,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(id = R.string.settings_pick_background))
                }
                Button(
                    onClick = onClearImage,
                    modifier = Modifier.weight(1f),
                    enabled = !backgroundImageUri.isNullOrBlank()
                ) {
                    Text(text = stringResource(id = R.string.settings_clear_background))
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(
    theme: WidgetTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, accentColor, textColor) = when (theme) {
        WidgetTheme.DARK_MINIMAL -> Triple(Color(0xFF1C1C1E), Color(0xFF6C5CE7), Color.White)
        WidgetTheme.NEON_CYBER -> Triple(Color(0xFF0D0D1A), Color(0xFF00FFFF), Color(0xFF00FFFF))
        WidgetTheme.SOFT_PASTEL -> Triple(Color(0xFFF8F4F0), Color(0xFF98D8AA), Color(0xFF4A4A4A))
    }

    Card(
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, AccentPrimary, RoundedCornerShape(16.dp))
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(accentColor.copy(alpha = 0.3f), CircleShape)
                        .border(2.dp, accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.settings_theme_preview_time),
                        fontSize = 8.sp,
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(id = theme.shortNameRes()),
                    fontSize = 10.sp,
                    color = textColor.copy(alpha = 0.8f)
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .background(AccentPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(id = R.string.content_desc_selected),
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .then(
                if (selected) {
                    Modifier.border(2.dp, AccentPrimary, RoundedCornerShape(14.dp))
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) AccentPrimary.copy(alpha = 0.15f) else CardBackground
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) AccentPrimary else MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun GoalSlider(
    currentGoalHours: Float,
    onGoalChange: (Float) -> Unit
) {
    val minHours = 1
    val maxHours = 8
    val currentHours = currentGoalHours.toInt()

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = pluralStringResource(id = R.plurals.goal_hours, count = minHours, minHours),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(
                text = pluralStringResource(id = R.plurals.goal_hours, count = currentHours, currentHours),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AccentPrimary
            )
            Text(
                text = pluralStringResource(id = R.plurals.goal_hours, count = maxHours, maxHours),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        Slider(
            value = currentGoalHours,
            onValueChange = onGoalChange,
            valueRange = 1f..8f,
            steps = 6,
            colors = SliderDefaults.colors(
                thumbColor = AccentPrimary,
                activeTrackColor = AccentPrimary,
                inactiveTrackColor = CardBackground
            )
        )
    }
}

@Composable
private fun MetricSelector(
    selectedMetric: AppListMetric,
    onMetricSelected: (AppListMetric) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricOption(
            title = stringResource(id = R.string.settings_list_metric_usage_time),
            selected = selectedMetric == AppListMetric.USAGE_TIME,
            onClick = { onMetricSelected(AppListMetric.USAGE_TIME) }
        )
        MetricOption(
            title = stringResource(id = R.string.settings_list_metric_launch_count),
            selected = selectedMetric == AppListMetric.LAUNCH_COUNT,
            onClick = { onMetricSelected(AppListMetric.LAUNCH_COUNT) }
        )
        MetricOption(
            title = stringResource(id = R.string.settings_list_metric_usage_percentage),
            selected = selectedMetric == AppListMetric.USAGE_PERCENTAGE,
            onClick = { onMetricSelected(AppListMetric.USAGE_PERCENTAGE) }
        )
    }
}

@Composable
private fun MetricOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .then(
                if (selected) {
                    Modifier.border(2.dp, AccentPrimary, RoundedCornerShape(14.dp))
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) AccentPrimary.copy(alpha = 0.16f) else CardBackground
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) AccentPrimary else MaterialTheme.colorScheme.onBackground,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AccentPrimary,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = CardBackground
                )
            )
        }
    }
}

@Composable
private fun ExcludedPackagesEditor(
    inputValue: String,
    savedPackagesCount: Int,
    validationError: String?,
    onInputChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.settings_excluded_packages_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.settings_excluded_packages_desc),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = inputValue,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8,
                singleLine = false,
                placeholder = {
                    Text(text = stringResource(id = R.string.settings_excluded_packages_placeholder))
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        id = R.string.settings_excluded_packages_count,
                        savedPackagesCount
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Button(onClick = onSave) {
                    Text(text = stringResource(id = R.string.settings_save_excluded_packages))
                }
            }

            if (validationError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = validationError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (savedPackagesCount == 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.settings_excluded_packages_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun AboutOption(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.settings_about_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.settings_about_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AccentPrimary.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.settings_battery_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.settings_battery_desc),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
