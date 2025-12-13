package com.abhinavvaidya.appusagetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abhinavvaidya.appusagetracker.ui.theme.AccentPrimary
import com.abhinavvaidya.appusagetracker.ui.theme.GradientPink

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 20.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // Gradient accent bar
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 24.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(AccentPrimary, GradientPink)
                    )
                )
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

