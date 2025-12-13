package com.abhinavvaidya.appusagetracker.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Premium Dark Theme Colors - Elegant & Modern
val DarkBackground = Color(0xFF0A0A0F)
val DarkSurface = Color(0xFF12121A)
val DarkCard = Color(0xFF1A1A24)
val DarkCardElevated = Color(0xFF22222E)

// Vibrant Accent Colors - Gradient Palette
val AccentPrimary = Color(0xFF7C3AED) // Vibrant Purple
val AccentSecondary = Color(0xFF06B6D4) // Cyan
val AccentTertiary = Color(0xFFF472B6) // Pink
val AccentSuccess = Color(0xFF10B981) // Emerald
val AccentWarning = Color(0xFFFBBF24) // Amber
val AccentError = Color(0xFFEF4444) // Red

// Gradient Colors
val GradientPurple = Color(0xFF8B5CF6)
val GradientPink = Color(0xFFEC4899)
val GradientBlue = Color(0xFF3B82F6)
val GradientCyan = Color(0xFF06B6D4)
val GradientIndigo = Color(0xFF6366F1)

// Text Colors
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Surface Colors with transparency for glassmorphism
val GlassSurface = Color(0x1AFFFFFF)
val GlassBorder = Color(0x33FFFFFF)

// Gradient Brushes
val PrimaryGradient = Brush.linearGradient(
    colors = listOf(AccentPrimary, GradientPink)
)

val CoolGradient = Brush.linearGradient(
    colors = listOf(GradientBlue, GradientCyan)
)

val WarmGradient = Brush.linearGradient(
    colors = listOf(GradientPink, AccentWarning)
)

val CardGradient = Brush.verticalGradient(
    colors = listOf(DarkCard, DarkCardElevated)
)

// Usage level colors
val UsageLow = Color(0xFF10B981)
val UsageMedium = Color(0xFFFBBF24)
val UsageHigh = Color(0xFFEF4444)
